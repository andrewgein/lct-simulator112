from threading import Lock

import grpc
from google.protobuf import json_format
from google.protobuf.empty_pb2 import Empty

from app.grpc.com.simulator112.context.context_service_pb2 import (
    COMPLETED, DISCONNECTED, IDLE, IN_CALL, DialogContext, DialogProgress,
)
from app.grpc.com.simulator112.incident.incident_context_pb2 import IncidentContext


_lock = Lock()
_progress = {}
_transcripts = {}


class NoMoreDialups(grpc.RpcError):
    def code(self):
        return grpc.StatusCode.NOT_FOUND

    def details(self):
        return "No more dialups"


def get_context(context_id) -> IncidentContext:
    context = IncidentContext()
    json_format.Parse(incident_json, context)
    return context


def send_dialog_context(context_id, dialog_data: DialogContext):
    _transcripts.setdefault(context_id, DialogContext()).transcript.extend(dialog_data.transcript)


def call(method_name, request):
    with _lock:
        context_id = request.uuid if hasattr(request, "uuid") else request.context_id
        if method_name == "GetIncidentContext":
            return get_context(context_id)
        if method_name == "AppendDialogContext":
            send_dialog_context(context_id, request.dialog_context)
            return Empty()
        if method_name == "GetDialup":
            for stage in get_context(context_id).stages:
                for dialup in stage.dialups:
                    if dialup.id == request.dialup_id:
                        return dialup
            raise ValueError("Dialup does not belong to context")

        progress = _progress.setdefault(context_id, DialogProgress(context_id=context_id, status=IDLE))
        if method_name == "GetNextDialup":
            dialups = [
                dialup
                for stage in sorted(get_context(context_id).stages, key=lambda stage: stage.position)
                for dialup in sorted(stage.dialups, key=lambda dialup: dialup.position)
            ]
            index = 0
            if request.current_dialup_id != "-1":
                ids = [dialup.id for dialup in dialups]
                if request.current_dialup_id not in ids:
                    raise ValueError("Dialup does not belong to context")
                index = ids.index(request.current_dialup_id) + 1
            if index >= len(dialups):
                raise NoMoreDialups()
            return dialups[index]

        if method_name == "StartDialup":
            ids = {dialup.id for stage in get_context(context_id).stages for dialup in stage.dialups}
            if request.dialup_id not in ids:
                raise ValueError("Dialup does not belong to context")
            if progress.status in (IN_CALL, DISCONNECTED) and progress.active_dialup_id != request.dialup_id:
                raise ValueError("Another dialup is active")
            progress.active_dialup_id = request.dialup_id
            progress.status = IN_CALL
        elif method_name in ("CompleteDialup", "DisconnectDialup"):
            if not progress.active_dialup_id or progress.active_dialup_id != request.dialup_id:
                raise ValueError("Dialup is not active")
            if method_name == "CompleteDialup":
                progress.status = COMPLETED
            elif progress.status == IN_CALL:
                progress.status = DISCONNECTED
        elif method_name != "GetDialogProgress":
            raise ValueError(f"Unknown mock method: {method_name}")

        result = DialogProgress()
        result.CopyFrom(progress)
        return result


incident_json = """
{
  "id": "RU-112-MOCK-MEDICAL-001",
  "title": "Маленькая девочка нашла маму без сознания",
  "address": {
    "city": "Москва",
    "street": "улица Академика Королёва",
    "house": "14",
    "apartment": "87",
    "floor": 8
  },
  "stages": [
    {
      "id": "mock-stage-1",
      "position": 1,
      "description": "Маленькая девочка позвонила в службу 112: её мама внезапно упала в обморок и не отвечает.",
      "dialups": [
        {
          "id": "mock-dialup-1",
          "position": 1,
          "applicant": {
            "firstName": "София",
            "lastName": "Иванова",
            "age": 8,
            "phone": "8 (961) 263-36-64",
            "emotionalState": "PANICKED",
            "address": "Москва, улица Академика Королёва, дом 14, квартира 87, восьмой этаж"
          },
          "victim": {
            "firstName": "Анна",
            "lastName": "Иванова",
            "address": "Москва, улица Академика Королёва, дом 14, квартира 87, восьмой этаж"
          },
          "dialupDetails": {
            "gender": "WOMEN",
            "emotionalState": "PANICKED",
            "knownFacts": [
              "Заявитель — девочка София, ей восемь лет",
              "Девочка находится дома вместе с мамой",
              "Мама внезапно упала и сейчас не отвечает на голос и прикосновения",
              "Девочка не знает, что делать, и очень напугана",
              "Адрес: Москва, улица Академика Королёва, дом четырнадцать, квартира восемьдесят семь",
              "Квартира находится на восьмом этаже",
              "Девочка может открыть дверь прибывающим сотрудникам"
            ],
            "hiddenFacts": [
              "Девочка не умеет измерять пульс и давление",
              "Неизвестно, дышит ли мама — это нужно спокойно уточнить и проверить",
              "Рядом нет другого взрослого, который мог бы помочь",
              "Телефон находится у девочки, заряд батареи достаточный",
              "В квартире нет дыма, огня или запаха газа"
            ],
            "aiContext": "Вы — маленькая девочка София, вам восемь лет. Вы очень испуганы и плачете, потому что мама упала в обморок и не отвечает. Говорите короткими детскими фразами, иногда сбивайтесь, но отвечайте на вопросы оператора. Не придумывайте факты и не подсказывайте оператору правильные действия. В начале скажите только, что мама упала и не отвечает. Если оператор просит проверить дыхание, осторожно посмотрите, поднимается ли грудь, и сообщите результат. Если оператор говорит позвать взрослого, позвонить соседям или открыть дверь, подтвердите только после выполнения. Назовите адрес только после соответствующего вопроса: Москва, улица Академика Королёва, дом четырнадцать, квартира восемьдесят семь, восьмой этаж. Не утверждайте, что мама умерла или получила конкретную травму, если оператор этого не установил."
          }
        }
      ]
    }
  ],
  "dispatcherCriteria": {
    "requiredQuestions": [
      "Мама дышит? Поднимается ли её грудь?",
      "Мама реагирует на голос или прикосновение?",
      "Какой точный адрес и этаж?",
      "Есть ли рядом взрослый, сосед или родственник?",
      "В квартире есть дым, огонь, запах газа или другая опасность?"
    ],
    "expectedActions": [
      "Немедленно направить бригаду скорой медицинской помощи",
      "При отсутствии нормального дыхания дать инструкции по вызову взрослого и сердечно-лёгочной реанимации с учётом возраста заявителя",
      "Попросить девочку открыть входную дверь, если это безопасно",
      "Оставаться на линии и говорить спокойными короткими фразами"
    ],
    "criticalMistakes": [
      "Завершить разговор без уточнения дыхания и точного адреса",
      "Оставить ребёнка без инструкций и немедленного вызова скорой помощи",
      "Попросить девочку выполнять опасные действия или давать лекарства",
      "Утверждать причину обморока или смерть матери без подтверждения"
    ]
  }
}
"""
