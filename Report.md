# Large-Scale Data Analysis Using MapReduce

## 1. Approach and MapReduce Logic
The objective was to analyze the **RAWG Game Dataset**, which contains metadata for over 470,000 games, and determine the **Total Count of Games** and **Average Rating** across different game genres. This addresses a real-world analytics task to understand trends in game development.

The MapReduce logic was implemented natively in **Java**.
- **Mapper (`GameAnalytics.GameMapper`)**: Safely parses commas inside quoted text fields. It skips the header and extracts the `rating` (column index 8) and `genres` (column index 18). Since a game can belong to multiple genres (delimited by `||`), the mapper tokenizes them and emits key-value pairs of `(Genre, "1\tRating")`. 
- **Reducer (`GameAnalytics.GameReducer`)**: As Hadoop guarantees sorted keys, the reducer groups all values by `Genre`. It parses the tab-separated string inside the value, accumulates the total count and rating for each genre iteration, and calculates the true average rating by dividing total rating by total game count for that genre, finally emitting `(Genre, TotalCount\tAverageRating)`.

## 2. Insights and Results 
Upon executing this MapReduce job against the full dataset, the results were aggregated into 19 distinct genres. `Action` leads as the most populated genre with **102,028 games**, followed by `Adventure` (72,211), `Puzzle` (55,552), and `Platformer` (48,254). Educational and Massively Multiplayer games are the least popular to produce, featuring counts below 8,000. 

A fascinating finding arises when evaluating the average ratings. The **Massively Multiplayer** genre, despite having the smallest catalog (2,289 titles), yields the highest average rating (0.48), possibly pointing to active community engagement that results in higher proportional reviewing. Conversely, saturated markets like Action and Adventure hold extremely low average ratings (~0.18). The generally low numeric average across the board reveals a secondary data pattern: an overwhelming majority of obscure games in this database have zero ratings or missing scores, dragging the mathematical average towards zero.

## 3. Performance & Accuracy Tracking 
The overall Hadoop MapReduce task was evaluated on a local pseudo-distributed Docker deployment. Processing almost half a million records and dynamically parsing safely with Java MapReduce Job logic took 16 seconds map execution time and 1.7 seconds reduce execution time, showcasing native Java framework scaling performance throughput. Memory utilization peaked at approximately `2.4 GB` Virtual Memory for map and reduce tasks indicating efficiency. 
To improve accuracy, the model can be expanded by filtering out "Zero-rated" or "Null-rated" games from the average calculation. Also, passing a secondary condition (e.g. limiting the average calculation to games with `ratings_count > 100`) would offer a realistic player reception representation rather than factoring in unnoticed indie uploads. The Mapper logic can be easily enriched to implement this filter.
