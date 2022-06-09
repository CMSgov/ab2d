package gov.cms.ab2d.worker.util;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

public class SnsMockUtil {
    public static void mockSns(AmazonSNS amazonSns) {
        CreateTopicResult result = new CreateTopicResult();
        result.setTopicArn("");
        Mockito.when(amazonSns.createTopic(anyString())).thenReturn(result);
        SubscribeResult results = new SubscribeResult();
        results.setSubscriptionArn("");
        Mockito.when(amazonSns.subscribe(anyString(), anyString(), anyString())).thenReturn(new SubscribeResult());
    }

    public static void mockSqs(AmazonSQS amazonSqs) {
        CreateQueueResult result = new CreateQueueResult();
        result.setQueueUrl("");
        Mockito.when(amazonSqs.createQueue(anyString())).thenReturn(result);
        Mockito.when(amazonSqs.getQueueAttributes(anyString(), anyList())).thenReturn(new GetQueueAttributesResult());

    }
}
