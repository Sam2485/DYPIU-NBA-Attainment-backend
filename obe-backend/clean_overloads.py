import re

def process_approval_controller():
    with open('src/main/java/com/dypiu/nba/controller/ApprovalController.java', 'r') as f:
        lines = f.readlines()
        
    # We will remove the overloaded method that just delegates
    # public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getApprovals(
    #        @RequestParam(required = false) String role, ...
    
    # Actually it's easier to just edit the file directly using text matching since it's just a few lines
    content = "".join(lines)
    # The first getApprovals just delegates to the second one. Let's find it.
    pattern = r'@GetMapping\(""\)\s*public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getApprovals\([\s\S]*?return getApprovals\([\s\S]*?;\s*\}'
    content = re.sub(pattern, '', content, count=1)
    
    with open('src/main/java/com/dypiu/nba/controller/ApprovalController.java', 'w') as f:
        f.write(content)

process_approval_controller()
