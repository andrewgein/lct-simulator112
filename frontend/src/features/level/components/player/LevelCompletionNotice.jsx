export default function LevelCompletionNotice({ complete, ready, finishing, onFinish }) {
  const variant = complete ? "success" : "warning";
  return (
    <wa-callout variant={variant}>
      <wa-icon slot="icon" name={complete ? "circle-check" : "triangle-exclamation"}></wa-icon>
      <div class="wa-split wa-align-items-center">
        <span>{complete ? "Все вызовы обработаны. Проверьте карточки и завершите уровень, когда будете готовы." : "Не все карточки заполнены. Вы можете завершить уровень, но незаполненные данные повлияют на результат."}</span>
        <wa-button type="button" variant={variant} disabled={!ready} loading={finishing} onClick={onFinish}>Завершить уровень</wa-button>
      </div>
    </wa-callout>
  );
}
