package uk.bl.wap.hadoop.tika;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.util.Tool;

import uk.bl.wap.hadoop.TextOutputFormat;
import uk.bl.wap.hadoop.WARCFileInputFormat;
import uk.bl.wap.util.solr.SolrRecord;

/**
 * WARCTikExtractor
 * Extracts text using Tika from a series of WARC files.
 * 
 * @author rcoram
 */

@SuppressWarnings( { "deprecation" } )
public class WARCTikaExtractor extends Configured implements Tool {

	public int run( String[] args ) throws IOException {
		JobConf conf = new JobConf( getConf(), WARCTikaExtractor.class );
		String line = null;

		BufferedReader br = new BufferedReader( new FileReader( args[ 0 ] ) );

		HashMap<String, Integer> tiMap = new HashMap<String, Integer>();

		while( ( line = br.readLine() ) != null ) {
			FileInputFormat.addInputPath( conf, new Path( line ) );
			String val[] = line.split( "/" );
			// Count the number of Target Instances in this batch and set number
			// of reducers accordingly
			tiMap.put( this.getWctTi( val[ val.length - 1 ] ), 1 );
		}

		FileOutputFormat.setOutputPath( conf, new Path( args[ 1 ] ) );
		
		if( args.length > 2 ) {
			for( int i = 2; i < args.length; i++ ) {
				try {
					DistributedCache.addCacheFile( new URI( args[ i ] ), conf );
				} catch( URISyntaxException e ) {
					e.printStackTrace();
				}
			}
		}

		conf.setJobName( args[ 0 ] + "_" + System.currentTimeMillis() );
		conf.setInputFormat( WARCFileInputFormat.class );
		conf.setMapperClass( WARCTikaMapper.class );
		conf.setReducerClass( ArchiveTikaReducer.class );
		conf.setOutputFormat( TextOutputFormat.class );
		conf.set( "map.output.key.field.separator", "" );

		conf.setOutputKeyClass( Text.class );
		conf.setOutputValueClass( Text.class );
		conf.setMapOutputValueClass( SolrRecord.class );
		conf.setNumReduceTasks( tiMap.size() );
		JobClient.runJob( conf );
		return 0;

	}

	public static void main( String[] args ) throws Exception {
		if( !( args.length > 0 ) ) {
			System.out.println( "Need input file.list and output dir!" );
			System.exit( 0 );

		}
		int ret = ToolRunner.run( new WARCTikaExtractor(), args );

		System.exit( ret );
	}

	private String getWctTi( String warcName ) {
		Pattern pattern = Pattern.compile( "^BL-([0-9]+)-[0-9]+\\.warc(\\.gz)?$" );
		Matcher matcher = pattern.matcher( warcName );
		if( matcher.matches() ) {
			return matcher.group( 1 );
		}
		return "";
	}
}
