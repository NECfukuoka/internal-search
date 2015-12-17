#!/bin/bash
$CATALINA_HOME/bin/catalina.sh run &

/usr/sbin/httpd -D FOREGROUND
