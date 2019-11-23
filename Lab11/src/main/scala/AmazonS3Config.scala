import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

object AmazonS3Config {
  private val accessKeyId = "AKIAQJR62HKNUWSILCUZ"
  private val secretAccessKey = "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"
  private val clientRegion = Regions.EU_CENTRAL_1
  private val credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey)

  val bucketName = "zhalgas-task1"
  val bucketNameV2 = "zhalgas-task1"
  val pathS3 = "./src/main/resources/s3"

  val client: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .withRegion(clientRegion)
    .build()
}