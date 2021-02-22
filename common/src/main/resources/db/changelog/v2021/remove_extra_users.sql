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
            'xi43hl32sdfl3kfkljew', 'afwjil423fweohfwejkl', '0oa2t7wfhvB2qSqPg297',
            '0oa2t7wdgfB2qSRTg397', '0ca27wdgfKefjefTg534', '0ca27wcvnxkjjefTg334',
            '0ca27w535jfejefTg124', '0ca902fhewffhhhwg144', '039fuewmn44fhhhwg144'
        )
);

DELETE FROM user_account
WHERE username IN (
   '832hfwef93hfkkf3hhhh', '9fhselhffhf93jhflshh', '39fhklsfzxmneu32feff',
   'bvk329df23fhjfsdjkef', 'vdbewfjzjhfew93fh3hh', 'zvhjefio239fhiew2fff',
   'vxcliwefnl239f03f2jf', 'vzdhwfehj3249fwejkfh', 'vznkweicln349fn32rjj',
   'xi43hl32sdfl3kfkljew', 'afwjil423fweohfwejkl', '0oa2t7wfhvB2qSqPg297',
   '0oa2t7wdgfB2qSRTg397', '0ca27wdgfKefjefTg534', '0ca27wcvnxkjjefTg334',
   '0ca27w535jfejefTg124', '0ca902fhewffhhhwg144', '039fuewmn44fhhhwg144'
);