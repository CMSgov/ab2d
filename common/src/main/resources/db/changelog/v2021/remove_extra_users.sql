--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:remove_extra_users failOnError:true

DELETE FROM user_role
WHERE user_account_id IN (
    SELECT id FROM user_account WHERE enabled = false
);

DELETE FROM user_account
WHERE enabled = false;

DELETE FROM user_role
WHERE user_account_id IN (
        SELECT id FROM user_account WHERE username IN (
            '832hfwef93hfkkf3hhhh', '9fhselhffhf93jhflshh', '39fhklsfzxmneu32feff',
            'bvk329df23fhjfsdjkef', 'vdbewfjzjhfew93fh3hh', 'zvhjefio239fhiew2fff',
            'vxcliwefnl239f03f2jf', 'vzdhwfehj3249fwejkfh', 'vznkweicln349fn32rjj',
            'xi43hl32sdfl3kfkljew', 'afwjil423fweohfwejkl'
        )
);

DELETE FROM user_account
WHERE username IN (
   '832hfwef93hfkkf3hhhh', '9fhselhffhf93jhflshh', '39fhklsfzxmneu32feff',
   'bvk329df23fhjfsdjkef', 'vdbewfjzjhfew93fh3hh', 'zvhjefio239fhiew2fff',
   'vxcliwefnl239f03f2jf', 'vzdhwfehj3249fwejkfh', 'vznkweicln349fn32rjj',
   'xi43hl32sdfl3kfkljew', 'afwjil423fweohfwejkl'
);