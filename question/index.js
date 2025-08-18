document.getElementById("submit").addEventListener("click", async () => {
    // Get all input elements
    const inputs = document.querySelectorAll("input");

    // Build JSON object
    const jsonData = {
        data: {

        }
    };
    let hasEmpty = false;

    inputs.forEach((input) => {
        if (input.value.trim() === "") {
            hasEmpty = true;
        }
        jsonData.data[input.id] = input.value.trim(); // use input id as key
    });

    // Validation check
    if (hasEmpty) {
        alert("Please fill in all fields before submitting.");
        return;
    }

    try {
        const response = await fetch("https://addquestion-3n4ewh545a-uc.a.run.app", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(jsonData),
        });

        if (!response.ok) {
            throw new Error("Request failed with status " + response.status);
        }

        const data = await response.json();
        console.log("Response:", data);

        alert("Question added successfully!");

        inputs.forEach(input => input.value = "");
    } catch (err) {
        console.error("Error:", err);
        alert("Something went wrong. Please try again.");
    }
});
