document.addEventListener("DOMContentLoaded", async () => {
    try {
        const response = await fetch(
            "https://getrankings-3n4ewh545a-uc.a.run.app",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ data: {} }), // empty data
            }
        );

        if (!response.ok) {
            throw new Error("Request failed with status " + response.status);
        }

        const rankings = (await response.json()).result.rankings;
        console.log(rankings);

        // Example response:
        // [
        //   { "teamName": "Team A", "teamScore": 95 },
        //   { "teamName": "Team B", "teamScore": 88 }
        // ]

        // Create table
        const table = document.createElement("table");
        const borderColors = [
            "deepskyblue",
            "mediumvioletred",
            // "orange",
            "lightgreen",
            "mediumslateblue",
        ];

        // Create header row
        const headerRow = document.createElement("tr");
        ["Team Name", "Score"].forEach((text) => {
            const th = document.createElement("th");
            th.textContent = text;
            headerRow.appendChild(th);
        });
        table.appendChild(headerRow);

        // Add data rows
        rankings.forEach((team, index) => {
            const row = document.createElement("tr");

            const nameCell = document.createElement("td");
            nameCell.textContent = index + 1 + ". " + team.teamName;

            const scoreCell = document.createElement("td");
            scoreCell.textContent = team.teamScore;

            row.appendChild(nameCell);
            row.appendChild(scoreCell);

            let color = borderColors[index % borderColors.length];
            if (index == 0) color = "gold";
            else if (index == 1) color = "silver";
            else if (index == 2) color = "#CD7F32";
            row.querySelectorAll("td").forEach((td) => {
                td.style.border = `solid 1px ${color}`;
            });

            table.appendChild(row);
            if (index == 2){
                const marginRow = document.createElement("tr");
                marginRow.style.marginBottom = "20px"
                marginRow.style.display = "block";
                table.append(marginRow);
            }
        });

        // Remove spinner
        document.getElementsByClassName("loader-container")[0].remove();

        // Append table to body
        document.body.appendChild(table);
    } catch (err) {
        console.error("Error:", err);
        alert("Failed to load data");
    }
});
