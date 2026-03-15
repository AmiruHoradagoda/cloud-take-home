import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.text.DecimalFormat;

public class GameAnalytics {

    public static class GameMapper extends Mapper<Object, Text, Text, Text> {
        private final Text word = new Text();
        private final Text outValue = new Text();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            
            // Skip header
            if (line.startsWith("id,slug,name,metacritic")) {
                return;
            }

            // Split handling csv quotes
            String[] row = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            // Avoid OutOfBounds
            if (row.length <= 18) {
                return;
            }

            String ratingStr = row[8];
            String genresStr = row[18];

            if (genresStr == null || genresStr.trim().isEmpty() || genresStr.equals("\"\"")) {
                return; 
            }

            double rating = 0.0;
            try {
                if (!ratingStr.trim().isEmpty()) {
                    rating = Double.parseDouble(ratingStr.replace("\"", ""));
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors, default 0.0
            }

            String[] genres = genresStr.replace("\"", "").split("\\|\\|");

            for (String genre : genres) {
                genre = genre.trim();
                if (!genre.isEmpty()) {
                    word.set(genre);
                    outValue.set("1\t" + rating);
                    context.write(word, outValue);
                }
            }
        }
    }

    public static class GameReducer extends Reducer<Text, Text, Text, Text> {
        private final Text result = new Text();
        private static final DecimalFormat df = new DecimalFormat("0.00");

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            long totalCount = 0;
            double totalRating = 0.0;

            for (Text val : values) {
                String[] parts = val.toString().split("\t");
                if (parts.length == 2) {
                    try {
                        long count = Long.parseLong(parts[0]);
                        double rating = Double.parseDouble(parts[1]);
                        totalCount += count;
                        totalRating += rating;
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                }
            }

            if (totalCount > 0) {
                double avgRating = totalRating / totalCount;
                result.set(totalCount + "\t" + df.format(avgRating));
                context.write(key, result);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: GameAnalytics <input path> <output path>");
            System.exit(2);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Game Analytics");
        
        job.setJarByClass(GameAnalytics.class);
        job.setMapperClass(GameMapper.class);
        job.setReducerClass(GameReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
