# encoding: utf-8

include_controls "cis-aws-foundations-baseline" do

    control "cis-aws-foundations-1.14" do
       describe "Skipping cis-aws-foundations-1.14" do
          skip "The root account is not managed by the AB2D team and thus AB2D does not have access to this account. This test is N/A but should be addressed by GDIT."
       end
    end

    control "cis-aws-foundations-1.18" do
       describe "Skipping cis-aws-foundations-1.18" do
          skip "IAM Master and IAM Manager roles are not managed by the AB2D team and thus AB2D does not have access to this account. This test is N/A but should be addressed by GDIT."
       end
    end

    control "cis-aws-foundations-1.24" do
       describe "Skipping cis-aws-foundations-1.24" do
          skip "Administrator Access required for various members of the AB2D team to manage the AWS environment."
       end
    end
end
