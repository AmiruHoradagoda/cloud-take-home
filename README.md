# Cloud Computing Assignment - Large-Scale Data Analysis Using MapReduce

## Project Overview
This project processes the RAWG Game Dataset (containing over 470,000 games) using a custom Hadoop MapReduce job implemented in Java to extract insights regarding game genres, including the total count of games per genre and the average rating for each genre.

## Files Structure
- `GameAnalytics.java`: Java MapReduce implementation
- `game_info.csv`: The RAWG Game Dataset (Input)
- `results.txt`: Output collected from MapReduce Job
- `execution_logs.txt`: Log records of the job execution
- `Report.md`: Approach, results, and insights.

## Steps to Execute on Docker Hadoop
1. **Start Hadoop in Docker**: Make sure your container is running, and NameNode/DataNode/YARN are up.
   ```bash
   # Enter the container
   docker exec -it hadoop-env /bin/bash
   # If not running, format NameNode and start all:
   /usr/local/hadoop/bin/hdfs namenode -format
   /usr/local/hadoop/sbin/start-all.sh
   ```

2. **Prepare Files**: Copy code and data into the container and compile the Java code.
   ```bash
   # From host, copy items to container
   docker cp game_info.csv hadoop-env:/game_info.csv
   docker cp GameAnalytics.java hadoop-env:/GameAnalytics.java

   # Compile the Java MapReduce code inside the container
   docker exec -it hadoop-env bash
   mkdir -p /build
   javac -classpath $(/usr/local/hadoop/bin/hadoop classpath) -d /build /GameAnalytics.java
   jar -cvf /GameAnalytics.jar -C /build/ .
   ```

3. **Upload Dataset to HDFS**:
   ```bash
   docker exec hadoop-env /usr/local/hadoop/bin/hdfs dfs -mkdir -p /input
   docker exec hadoop-env /usr/local/hadoop/bin/hdfs dfs -put /game_info.csv /input/
   ```

4. **Run Hadoop Java Job**:
   ```bash
   docker exec hadoop-env /usr/local/hadoop/bin/hadoop jar /GameAnalytics.jar GameAnalytics /input/game_info.csv /output_java
   ```

5. **Retrieve the Results**:
   ```bash
   docker exec hadoop-env /usr/local/hadoop/bin/hdfs dfs -get /output_java/part-r-00000 /tmp/results.txt
   docker cp hadoop-env:/tmp/results.txt results.txt
   ```
