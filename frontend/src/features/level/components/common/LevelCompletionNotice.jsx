const DEFAULT_COMPLETE_MESSAGE = "Все вызовы обработаны. Проверьте карточки и завершите уровень, когда будете готовы.";
const DEFAULT_INCOMPLETE_MESSAGE = "Не все карточки заполнены. Вы можете завершить уровень, но незаполненные данные повлияют на результат.";

export default function LevelCompletionNotice({ complete, ready, finishing, onFinish, completeMessage = DEFAULT_COMPLETE_MESSAGE, incompleteMessage = DEFAULT_INCOMPLETE_MESSAGE, className = "" }) {
  const variant = complete ? "success" : "warning";
  return (
    <wa-callout class={className} variant={variant}>
      <wa-icon slot="icon" name={complete ? "circle-check" : "triangle-exclamation"}></wa-icon>
      <div class="wa-split wa-align-items-center">
        <span>{complete ? completeMessage : incompleteMessage}</span>
        <wa-button type="button" variant={variant} disabled={!ready} loading={finishing} onClick={onFinish}>Завершить уровень</wa-button>
      </div>
    </wa-callout>
  );
}
