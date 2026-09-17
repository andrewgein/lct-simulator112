export default function InstructionsCard({ incident }) {
  return (
    <wa-card style="flex: 1">
      <div class="wa-stack">
        <strong class="wa-heading-l">Инструкции</strong>
        <div class="wa-stack">
          {!!incident?.instructions?.length && (
            <ul>
              {incident.instructions.map((instruction) => <li key={instruction}>{instruction}</li>)}
            </ul>
          )}
        </div>
      </div>
    </wa-card>
  );
}
