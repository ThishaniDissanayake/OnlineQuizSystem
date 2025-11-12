export function createQuestionCard(question, index) {
  const wrapper = document.createElement("div");
  wrapper.className = "question-card";

  const questionText = document.createElement("h3");
  questionText.textContent = `${index}. ${question.question}`;

  const optionsList = document.createElement("div");
  optionsList.className = "options-list";

  question.options.forEach((opt, i) => {
    const label = document.createElement("label");
    label.className = "option";

    const input = document.createElement("input");
    input.type = "radio";
    input.name = `q${question.id}`;
    input.value = opt;

    label.appendChild(input);
    label.append(opt);
    optionsList.appendChild(label);
  });

  wrapper.appendChild(questionText);
  wrapper.appendChild(optionsList);
  return wrapper;
}
