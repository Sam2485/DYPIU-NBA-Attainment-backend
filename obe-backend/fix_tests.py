import re

def fix(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # The actual replacements needed are:
    # getProgrammeCoordinatorDashboard( X, null ) -> getProgrammeCoordinatorDashboard( X, null, null )
    # getCourseCoordinatorDashboard( X, null, principal ) -> getCourseCoordinatorDashboard( X, null, null, principal )
    
    content = content.replace(
        'dashboardController.getProgrammeCoordinatorDashboard(progA1.getId(), null)',
        'dashboardController.getProgrammeCoordinatorDashboard(progA1.getId(), null, null)'
    )
    content = content.replace(
        'dashboardController.getProgrammeCoordinatorDashboard(progA2.getId(), null)',
        'dashboardController.getProgrammeCoordinatorDashboard(progA2.getId(), null, null)'
    )
    content = content.replace(
        'dashboardController.getProgrammeCoordinatorDashboard(progB1.getId(), null)',
        'dashboardController.getProgrammeCoordinatorDashboard(progB1.getId(), null, null)'
    )
    
    content = content.replace(
        'dashboardController.getCourseCoordinatorDashboard(courseA1.getId(), null, mockPrincipal)',
        'dashboardController.getCourseCoordinatorDashboard(courseA1.getId(), null, null, mockPrincipal)'
    )
    content = content.replace(
        'dashboardController.getCourseCoordinatorDashboard(courseA2.getId(), null, mockPrincipal)',
        'dashboardController.getCourseCoordinatorDashboard(courseA2.getId(), null, null, mockPrincipal)'
    )
    content = content.replace(
        'dashboardController.getCourseCoordinatorDashboard(courseB1.getId(), null, mockPrincipal)',
        'dashboardController.getCourseCoordinatorDashboard(courseB1.getId(), null, null, mockPrincipal)'
    )

    with open(filepath, 'w') as f:
        f.write(content)

fix('src/test/java/com/dypiu/nba/security/ProgrammeCoordinatorScopeSecurityTest.java')
fix('src/test/java/com/dypiu/nba/security/CourseCoordinatorScopeSecurityTest.java')
