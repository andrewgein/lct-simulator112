import ClassifierCategoryOptions from "./ClassifierCategoryOptions.jsx";

export default function ClassifierOptions({ classifier, selectedValue, excludedValues = [] }) {
  const groups = classifier.map((category) => ({
    ...category,
    entries: category.entries.filter((entry) => entry.code === selectedValue || !excludedValues.includes(entry.code))
  })).filter((category) => category.entries.length > 0);

  return groups.map((category, index) => (
    <div class="classifier-category" key={category.code}>
      {index > 0 && <wa-divider></wa-divider>}
      <small class="classifier-group-label" aria-hidden="true">{category.name}</small>
      <ClassifierCategoryOptions category={category} />
    </div>
  ));
}
