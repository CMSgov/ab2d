package gov.cms.ab2d.worker.util;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeResult;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyString;

public class SnsMockUtil {
    public static void mockSns(AmazonSNS amazonSns) {
        CreateTopicResult result = new CreateTopicResult();
        result.setTopicArn("");
        Mockito.when(amazonSns.createTopic(anyString())).thenReturn(result);
        Mockito.when(amazonSns.subscribe(anyString(), anyString(), anyString())).thenReturn(new SubscribeResult());
    }
}
