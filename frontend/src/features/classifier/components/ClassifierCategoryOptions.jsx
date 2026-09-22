function getGroup(entry) {
  const separatorIndex = entry.finalName.indexOf(":");
  const fallbackName = separatorIndex > 0 ? entry.finalName.slice(0, separatorIndex).trim() : "";
  const name = entry.feature1Name || fallbackName;
  return { key: entry.feature1Code || name, name };
}

function getOptionLabel(entry, groupName) {
  if (!groupName) return entry.finalName;
  const prefix = `${groupName}:`;
  return entry.finalName.toLocaleLowerCase().startsWith(prefix.toLocaleLowerCase()) ? entry.finalName.slice(prefix.length).trim() : entry.finalName;
}

export default function ClassifierCategoryOptions({ category }) {
  const groups = [];

  category.entries.forEach((entry) => {
    const group = getGroup(entry);
    let target = groups.find((item) => item.key === group.key);
    if (!target) {
      target = { ...group, entries: [] };
      groups.push(target);
    }
    target.entries.push(entry);
  });

  return groups.map((group) => (
    <div class="classifier-subgroup" key={group.key || "other"}>
      {group.name && <small class="classifier-subgroup-label" aria-hidden="true">{group.name}</small>}
      {group.entries.map((entry) => <wa-option key={entry.code} value={entry.code} label={entry.finalName}>{getOptionLabel(entry, group.name)}</wa-option>)}
    </div>
  ));
}
