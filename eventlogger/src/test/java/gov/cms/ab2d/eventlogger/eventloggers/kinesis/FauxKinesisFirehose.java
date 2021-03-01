package gov.cms.ab2d.eventlogger.eventloggers.kinesis;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.CreateDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.CreateDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.DeleteDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DeleteDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.ListDeliveryStreamsRequest;
import com.amazonaws.services.kinesisfirehose.model.ListDeliveryStreamsResult;
import com.amazonaws.services.kinesisfirehose.model.ListTagsForDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.ListTagsForDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.amazonaws.services.kinesisfirehose.model.StartDeliveryStreamEncryptionRequest;
import com.amazonaws.services.kinesisfirehose.model.StartDeliveryStreamEncryptionResult;
import com.amazonaws.services.kinesisfirehose.model.StopDeliveryStreamEncryptionRequest;
import com.amazonaws.services.kinesisfirehose.model.StopDeliveryStreamEncryptionResult;
import com.amazonaws.services.kinesisfirehose.model.TagDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.TagDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.UntagDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.UntagDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.UpdateDestinationRequest;
import com.amazonaws.services.kinesisfirehose.model.UpdateDestinationResult;

public class FauxKinesisFirehose implements AmazonKinesisFirehose {

    public Record latestRecord;

    @Override
    public void setEndpoint(String s) {

    }

    @Override
    public void setRegion(Region region) {

    }

    @Override
    public CreateDeliveryStreamResult createDeliveryStream(CreateDeliveryStreamRequest createDeliveryStreamRequest) {
        return null;
    }

    @Override
    public DeleteDeliveryStreamResult deleteDeliveryStream(DeleteDeliveryStreamRequest deleteDeliveryStreamRequest) {
        return null;
    }

    @Override
    public DescribeDeliveryStreamResult describeDeliveryStream(DescribeDeliveryStreamRequest describeDeliveryStreamRequest) {
        return null;
    }

    @Override
    public ListDeliveryStreamsResult listDeliveryStreams(ListDeliveryStreamsRequest listDeliveryStreamsRequest) {
        return null;
    }

    @Override
    public ListTagsForDeliveryStreamResult listTagsForDeliveryStream(ListTagsForDeliveryStreamRequest listTagsForDeliveryStreamRequest) {
        return null;
    }

    @Override
    public PutRecordResult putRecord(PutRecordRequest putRecordRequest) {
        this.latestRecord = putRecordRequest.getRecord();
        System.out.println(putRecordRequest.getRecord());
        return null;
    }

    @Override
    public PutRecordBatchResult putRecordBatch(PutRecordBatchRequest putRecordBatchRequest) {
        return null;
    }

    @Override
    public StartDeliveryStreamEncryptionResult startDeliveryStreamEncryption(StartDeliveryStreamEncryptionRequest startDeliveryStreamEncryptionRequest) {
        return null;
    }

    @Override
    public StopDeliveryStreamEncryptionResult stopDeliveryStreamEncryption(StopDeliveryStreamEncryptionRequest stopDeliveryStreamEncryptionRequest) {
        return null;
    }

    @Override
    public TagDeliveryStreamResult tagDeliveryStream(TagDeliveryStreamRequest tagDeliveryStreamRequest) {
        return null;
    }

    @Override
    public UntagDeliveryStreamResult untagDeliveryStream(UntagDeliveryStreamRequest untagDeliveryStreamRequest) {
        return null;
    }

    @Override
    public UpdateDestinationResult updateDestination(UpdateDestinationRequest updateDestinationRequest) {
        return null;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest amazonWebServiceRequest) {
        return null;
    }
}
