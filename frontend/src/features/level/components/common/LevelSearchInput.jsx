export default function LevelSearchInput({ value, onInput, hint, size = "l", iconSlot = "start" }) {
  return (
    <wa-input type="search" size={size} aria-label="Поиск происшествий" placeholder="Поиск происшествий" hint={hint} value={value} with-clear onInput={onInput}>
      <wa-icon slot={iconSlot} name="magnifying-glass"></wa-icon>
    </wa-input>
  );
}
