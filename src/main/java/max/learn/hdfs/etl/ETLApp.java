package max.learn.hdfs.etl;

import max.learn.hdfs.etl.utils.GetPageId;
import max.learn.hdfs.etl.utils.LogParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.Map;

/**
 * parse dummy log file and output
 */
public class ETLApp {

    // avoid error in Windows, load dll manually
    static {
        try {
            System.load("D:/hadoop-2.7.1/bin/hadoop.dll");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
            System.exit(1);
        }
    }

    // entry
    public static void main(String[] args) throws Exception{
        Configuration configuration = new Configuration();

        // 如果输出目录已经存在，则先删除
        FileSystem fileSystem = FileSystem.get(configuration);
        Path outputPath = new Path("data/etl/output/etl");
        if(fileSystem.exists(outputPath)) {
            fileSystem.delete(outputPath,true);
        }
        fileSystem.close();

        Job job = Job.getInstance(configuration);
        job.setJarByClass(ETLApp.class);
        job.setMapperClass(MyMapper.class);
        job.setMapOutputKeyClass(NullWritable.class);
        job.setMapOutputValueClass(Text.class);

        FileInputFormat.setInputPaths(job, new Path("data/etl/input/test-access.log"));
        FileOutputFormat.setOutputPath(job, new Path("data/etl/output/etl"));

        job.waitForCompletion(true);

        System.out.println("DONE, see output at: data/etl/output/etl");
        System.exit(1);
    }

    static class MyMapper extends Mapper<LongWritable, Text, NullWritable, Text> {

        private LogParser parser;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            parser = new LogParser();
        }

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String log = value.toString();
            Map<String, String> logInfo = parser.parse(log);

            String ip = logInfo.get("ip");
            String url = logInfo.get("url");
            String country = logInfo.get("country") == null ? "-" : logInfo.get("country");
            String province = logInfo.get("province")== null ? "-" : logInfo.get("province");
            String city = logInfo.get("city")== null ? "-" : logInfo.get("city");
            String pageId = GetPageId.getPageId(url)== "" ? "-" : GetPageId.getPageId(url);

            StringBuilder builder = new StringBuilder();
            builder.append(ip).append("\t");
            builder.append(url).append("\t");
            builder.append(country).append("\t");
            builder.append(province).append("\t");
            builder.append(city).append("\t");
            builder.append(pageId);

            context.write(NullWritable.get(), new Text(builder.toString()));
        }
    }
}


