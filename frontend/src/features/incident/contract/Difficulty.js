/** @enum {string} */
export const Difficulty = Object.freeze({
    EASY: "EASY",
    NORMAL: "NORMAL",
    HARD: "HARD"
});

/** @type {ReadonlyArray<{ value: string, label: string }>} */
export const DifficultyOptions = Object.freeze([
    { value: Difficulty.EASY, label: "Лёгкая" },
    { value: Difficulty.NORMAL, label: "Средняя" },
    { value: Difficulty.HARD, label: "Сложная" }
]);
