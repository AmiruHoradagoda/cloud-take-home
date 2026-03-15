#!/bin/bash

# Exit script if any command fails
set -e

CONTAINER_NAME="hadoop-env"
JAVA_FILE="GameAnalytics.java"
INPUT_CSV="game_info.csv"
HADOOP_BIN="/usr/local/hadoop/bin/hadoop"
HDFS_BIN="/usr/local/hadoop/bin/hdfs"

echo "================================================="
echo " Starting Hadoop MapReduce Analysis Execution    "
echo "================================================="

# 1. Check if the container is running
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "Error: Docker container '$CONTAINER_NAME' is not running."
    echo "Please start it using: docker start $CONTAINER_NAME"
    exit 1
fi

echo "Container '$CONTAINER_NAME' is running."

# 2. Start SSH and Hadoop Services inside the container
echo "⏳ Restarting SSH service and starting Hadoop Cluster..."
docker exec $CONTAINER_NAME bash -c "service ssh start || /etc/init.d/ssh start" || true

# Format NameNode if not formatted yet (doing this safely by checking if fsimage exists, or just skipping it for now since we did it)
# We will just start all.
docker exec $CONTAINER_NAME bash -c "/usr/local/hadoop/sbin/start-all.sh" || true
echo "Hadoop services started."

# 3. Copy files to the container
echo "Copying $JAVA_FILE and $INPUT_CSV to the raw container..."
if [ ! -f "$INPUT_CSV" ]; then
    echo "Error: $INPUT_CSV not found in local directory."
    exit 1
fi
docker cp "$JAVA_FILE" "$CONTAINER_NAME:/$JAVA_FILE"
docker cp "$INPUT_CSV" "$CONTAINER_NAME:/$INPUT_CSV"
echo "Files copied successfully."

# 4. Compile Java Code
echo "Compiling Java MapReduce code..."
docker exec $CONTAINER_NAME bash -c "
    rm -rf /build && mkdir -p /build
    javac -classpath \$($HADOOP_BIN classpath) -d /build /$JAVA_FILE
    jar -cvf /GameAnalytics.jar -C /build/ .
"
echo "Java compilation and JAR creation successful."

# 5. Upload Dataset to HDFS
echo "⏳ Uploading dataset to HDFS..."
docker exec $CONTAINER_NAME bash -c "
    $HDFS_BIN dfs -mkdir -p /input
    $HDFS_BIN dfs -put -f /$INPUT_CSV /input/
"
echo "Dataset available in HDFS at /input/$INPUT_CSV."

# 6. Execute Hadoop MapReduce Job
echo "Cleaning up previous output..."
docker exec $CONTAINER_NAME bash -c "$HDFS_BIN dfs -rm -r -f /output_java" || true

echo "EXECUTING MAPREDUCE JOB (This may take a moment)..."
docker exec $CONTAINER_NAME bash -c "$HADOOP_BIN jar /GameAnalytics.jar GameAnalytics /input/$INPUT_CSV /output_java"
echo "MapReduce execution finished successfully."

# 7. Collect and display output
echo "Formatting and fetching results..."
docker exec $CONTAINER_NAME bash -c "$HDFS_BIN dfs -cat /output_java/part-r-00000" > results.txt

echo "================================================="
echo "            MAPREDUCE PROCESS COMPLETE           "
echo "================================================="
echo "Top 10 Results Preview:"
head -n 10 results.txt

echo ""
echo "Full results have been saved locally to 'results.txt'."
