#!/bin/bash

# Fix status calculation
sed -i '' -e 's/ApprovalStatus status = "APPROVED".equalsIgnoreCase(statusValue) || "VERIFIED".equalsIgnoreCase(statusValue) ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING;/ApprovalStatus status; try { status = ApprovalStatus.valueOf(statusValue.toUpperCase()); } catch (IllegalArgumentException e) { status = "APPROVED".equalsIgnoreCase(statusValue) || "VERIFIED".equalsIgnoreCase(statusValue) ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING; }/g' /Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/ApprovalService.java

# Fix unconditional APPROVED assignment for Programme ATR
awk '
/if \(patr != null\) \{/ {
  if (in_prog_atr_block == 1) {
    print $0
    print "                if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.VERIFIED) {"
    print "                    patr.setStatus(com.dypiu.nba.entity.ProgrammeAtrStatus.APPROVED);"
    print "                } else if (status == ApprovalStatus.REJECTED || status == ApprovalStatus.REVISION_REQUESTED || status == ApprovalStatus.NEEDS_REVISION) {"
    print "                    patr.setStatus(com.dypiu.nba.entity.ProgrammeAtrStatus.NEEDS_REVISION);"
    print "                } else {"
    print "                    patr.setStatus(com.dypiu.nba.entity.ProgrammeAtrStatus.DRAFT);"
    print "                }"
    print "                patr.setVerifiedBy(verifierName);"
    print "                patr.setVerificationComments(remarksValue);"
    print "                programmeAtrRepository.save(patr);"
    print "            }"
    
    # skip the original block
    skip=6
    next
  }
}
/"programmeAtrStatus".equalsIgnoreCase\(statusType\)/ { in_prog_atr_block=1 }
skip > 0 { skip--; next }
{print}
' /Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/ApprovalService.java > temp_as.java
mv temp_as.java /Users/rajshaikh/Desktop/DYPIU-NBA-Attainment-backend/obe-backend/src/main/java/com/dypiu/nba/service/ApprovalService.java

