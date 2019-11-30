import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

object Config {
  private val accessKeyId = ""
  private val secretAccessKey = ""
  private val clientRegion = Regions.EU_CENTRAL_1
  private val credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey)

  val bucketName = "kbtu-kitchen-photo-service"

  val client: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(credentials))
    .withRegion(clientRegion)
    .build()
}
