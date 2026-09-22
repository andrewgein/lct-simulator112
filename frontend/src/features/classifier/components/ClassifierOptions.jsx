import ClassifierCategoryOptions from "./ClassifierCategoryOptions.jsx";

const styles = `
  .classifier-group-label {
    display: block;
    padding: var(--wa-space-xs) var(--wa-space-m) var(--wa-space-2xs);
    color: var(--wa-color-text-quiet);
    font-size: var(--wa-font-size-s);
    font-weight: var(--wa-font-weight-bold);
    text-transform: uppercase;
  }
`;

export default function ClassifierOptions({ classifier, selectedValue, excludedValues = [] }) {
  const groups = classifier.map((category) => ({
    ...category,
    entries: category.entries.filter((entry) => entry.code === selectedValue || !excludedValues.includes(entry.code))
  })).filter((category) => category.entries.length > 0);

  return (
    <>
      <style>{styles}</style>
      {groups.map((category, index) => (
        <div class="classifier-category" key={category.code}>
          {index > 0 && <wa-divider></wa-divider>}
          <small class="classifier-group-label" aria-hidden="true">{category.name}</small>
          <ClassifierCategoryOptions category={category} />
        </div>
      ))}
    </>
  );
}
