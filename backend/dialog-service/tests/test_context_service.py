import unittest
from unittest.mock import MagicMock, patch

from app.adapter.out.grpc.context_adapter import GrpcContextAdapter
from app.domain.model import CallDirection, CounterpartyType, Gender
from app.grpc.com.simulator112.context.context_service_pb2 import GetCallRequest
from app.grpc.com.simulator112.incident.incident_context_pb2 import (
    CALL_DIRECTION_OUTBOUND,
    COUNTERPARTY_TYPE_BRIGADE,
    GENDER_MAN,
    CallScenario,
    Person,
)


class ContextGrpcAdapterTests(unittest.TestCase):
    @patch.object(GrpcContextAdapter, "_call")
    def test_maps_new_call_contract_to_domain(self, call_mock: MagicMock):
        call_mock.return_value = CallScenario(
            id="call-1",
            position=2,
            direction=CALL_DIRECTION_OUTBOUND,
            counterparty=COUNTERPARTY_TYPE_BRIGADE,
            person=Person(first_name="Иван", age=42, phone="112", address="Москва, Тверская, 8"),
            gender=GENDER_MAN,
            known_facts=["Бригада прибыла"],
            emotional_state="CALM",
        )

        call = GrpcContextAdapter("localhost:9090").get_call("context-1", "call-1")

        self.assertEqual(CallDirection.OUTBOUND, call.direction)
        self.assertEqual(CounterpartyType.BRIGADE, call.counterparty)
        self.assertEqual(Gender.MAN, call.gender)
        self.assertEqual(42, call.person.age)
        self.assertEqual("Москва, Тверская, 8", call.person.address)
        method, request = call_mock.call_args.args
        self.assertEqual("GetCall", method)
        self.assertEqual(GetCallRequest(context_id="context-1", call_id="call-1"), request)


if __name__ == "__main__":
    unittest.main()
