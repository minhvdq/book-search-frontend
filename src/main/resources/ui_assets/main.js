$(document).ready(string => {
    console.log("ready")

    const button = document.getElementById("submit_button");
    const searchBox = document.getElementById("search_text");
    const numResultBox = document.getElementById("num_results");
    const minScoreBox = document.getElementById("min_score");
    const resultsTable = document.querySelector("#results table tbody");
    const resultsWrapper = document.getElementById("results");
    const noResultError = document.getElementById("no_result_error");

    button.addEventListener("click", () => {
        console.log("hello")
        $.ajax({
            method: "POST",
            contentType: "application/json",
            data: createRequest(),
            url: "document_search",
            dataType: "json",
            success: httpResponse
        })
    })

    const createRequest = () => {
        const searchQuery = searchBox.value;
        console.log(searchQuery, " is the search query");
        let minScore = parseFloat(minScoreBox.value, 10)
        console.log(minScore, " is the min score")

        if(isNaN(minScore)){
            minScore = 0;
        }

        let maxNumberOfResults = parseInt(numResultBox.value)
        if(isNaN(maxNumberOfResults)){
            maxNumberOfResults = Number.MAX_SAFE_INTEGER;
        }

        const frontendRequest = {
            search_query : searchQuery,
            min_score : minScore,
            max_number_of_results : maxNumberOfResults

        }
        console.log(JSON.stringify(frontendRequest))

        return JSON.stringify(frontendRequest);
    }

    const httpResponse = ( data, status ) => {
        if(status  === "success") {
            console.log(data)
            addResults(data);
        }
        else{
            alert("Error connecting to server " + status);
        }
    }
    const addResults = (data ) => {
        const dataDir = data.document_location
        resultsTable.innerHTML = '';

        if(data.search_results.length === 0){
            $("#no_result_error").show();
            $("#results").hide();
        }
        else{
            $("#no_result_error").hide();
            $("#results").show()
        }

        for( const result of data.search_results ){
            const title = result.title
            const extension = result.extension
            const score = result.score
            console.log(score, " is the score")
            const fullPath = dataDir + "/" + title + "." + extension;

            var tr = document.createElement("tr");
            var td_score = document.createElement("td");
            var td_link = document.createElement("td");
            var a = document.createElement("a");

            // Setting attributes for the link
            a.href = fullPath;
            a.title = title;
            a.textContent = title; // Set the link text to the title

            // Setting title attribute for the score cell
            td_score.textContent =  score;

            // Appending the link to the link cell
            td_link.append(a);

            // Appending cells to the table row
            tr.append(td_link);
            tr.append(td_score);

            // Appending the table row to the resultsTable (assuming resultsTable is a table element)
            resultsTable.append(tr);
        }
    }
})