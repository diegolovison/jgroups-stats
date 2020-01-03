# Build #
You need the latest https://github.com/belaban/JGroups master code and apply the PR https://github.com/belaban/JGroups/pull/476

# Test #
`mvn test`

# Result #
* `testSTATS`: It will fail because the STATS stats are most likely different because they only count the payload size (Message.getLength()), they ignore the protocol headers, and the "super-header" with the cluster name, version number, batch flag, and maybe something else...
* `testTrace`: It will works
* `testTPMessageStats`: It will works