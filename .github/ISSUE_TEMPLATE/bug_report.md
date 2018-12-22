---
name: Bug report
about: Create a report to help us improve
title: ''
labels: ''
assignees: ''

---

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

**Expected behavior**
A clear and concise description of what you expected to happen.

**Screenshots**
If applicable, add screenshots to help explain your problem.

**settings**
which type of idempotent support you use?(framework idempotent/business idempotent?)

which version and component do you use( redis/rdb log implement and redis/rdb version? kafka/ons queue implements and queue version? ribbon/dubbo rpc implements?)

**the relative records **
records in table executed_trans, idempotent, logs(trans_log_detail, trans_log_unfinished,or redis records)

**can you reproduce it in demos by changing certain settings**
this will help us understand the situation more effectively

**Additional context**
Add any other context about the problem here.
