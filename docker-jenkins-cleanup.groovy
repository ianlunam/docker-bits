pipeline {
    agent any
    stages {
        stage ("Checking Used Space") {
            steps {
                script {
                    // Get docker information from system
                    x = "docker info".execute().text

                    // Check storage driver:
                    for (line in x.split('\n')) {
                        trimmed = line.trim()
                        splitted = trimmed.split()
                        if (splitted.length >= 1) {
                            if (splitted[0] == 'Storage') {
                                if (splitted[2] != 'devicemapper') {
                                    error "This script only works if Storage Driver is devicemapper" 
                                }
                            }
                        }
                    }

                    AVAIL = x.split('\n')[14]
                    AVAIL = AVAIL.split()[3]
                    ALLOC = x.split('\n')[13]
                    ALLOC = ALLOC.split()[3]

                    print "Allocated space: " + ALLOC + "\nAvailable space: " + AVAIL

                    // Extract suffix and number from string
                    ALLOC_SUFFIX =  ALLOC.substring((ALLOC.size()-2), ALLOC.size())
                    ALLOC =  ALLOC.substring(0, (ALLOC.size()-2))
                    AVAIL_SUFFIX =  AVAIL.substring((AVAIL.size()-2), AVAIL.size())
                    AVAIL =  AVAIL.substring(0, (AVAIL.size()-2))

                    // Convert from whatever to bytes
                    switch (ALLOC_SUFFIX) {
                        case 'GB':
                            ALLOC = ALLOC.toFloat() * 1024 * 1024 * 1024
                            break
                        case 'MB':
                            ALLOC = ALLOC.toFloat() * 1024 * 1024
                            break
                        case 'KB':
                            ALLOC = ALLOC.toFloat() * 1024
                            break
                        case 'BS':
                            ALLOC = ALLOC.toFloat()
                            break
                    }

                    switch (AVAIL_SUFFIX) {
                        case 'GB':
                            AVAIL = AVAIL.toFloat() * 1024 * 1024 * 1024
                            break
                        case 'MB':
                            AVAIL = AVAIL.toFloat() * 1024 * 1024
                            break
                        case 'KB':
                            AVAIL = AVAIL.toFloat() * 1024
                            break
                        case 'BS':
                            AVAIL = AVAIL.toFloat()
                            break
                    }

                    // Calculate percentage
                    print "Allocated space (bytes): " + ALLOC + "\nAvailable space (bytes): " + AVAIL
                    PCT_REMAIN = ((AVAIL/ALLOC) * 100).round(2)
                    print "PCT Remaining: " + PCT_REMAIN + "%"
                }
            }
        }

        stage ("Clean up old stuff") {
            when {
                expression { PCT_REMAIN <= 10 }
            }
            steps {
                script {
                    // Remove all exited containers over 24hrs old
                    "docker container prune --force --filter until=24h".execute()

                    // Remove all unused images over 24hrs old
                    "docker image prune --all --force --filter until=24h".execute()

                    // Remove all unused volumes
                    "docker volume prune --force".execute()
                }
            }
        }
    }
}
