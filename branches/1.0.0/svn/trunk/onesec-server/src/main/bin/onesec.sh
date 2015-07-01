#! /bin/bash

#   Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
#   reserved.


# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home   
           fi
           ;;
esac

if [ -z "$ONESEC_HOME" ]; then
	echo Variable ONESEC_HOME not found
	exit 1
fi

# set OSSCON_LIB location
OSSCON_LIB=${ONESEC_HOME}/lib

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$ONESEC_HOME" ] &&
    ONESEC_HOME=`cygpath --unix "$ONESEC_HOME"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

if [ -z "$JAVACMD" ] ; then 
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then 
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=java
  fi
fi
 
if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

if [ -n "$CLASSPATH" ] ; then
  LOCALCLASSPATH="$CLASSPATH"
fi

for i in "${OSSCON_LIB}"/*.jar 
do
  # if the directory is empty, then it will return the input string
  # this is stupid, so case for it
  if [ "$i" != "${OSSCON_LIB}/*.jar" ] ; then
    if [ -z "$LOCALCLASSPATH" ] ; then
      LOCALCLASSPATH=$i
    else
      LOCALCLASSPATH="$i":"$LOCALCLASSPATH"
    fi
  fi
done

if [ -n "$JAVA_HOME" ] ; then
  if [ -f "$JAVA_HOME/lib/tools.jar" ] ; then
    LOCALCLASSPATH="$LOCALCLASSPATH:$JAVA_HOME/lib/tools.jar"
  fi

  if [ -f "$JAVA_HOME/lib/classes.zip" ] ; then
    LOCALCLASSPATH="$LOCALCLASSPATH:$JAVA_HOME/lib/classes.zip"
  fi
else
  echo "Warning: JAVA_HOME environment variable is not set."
  echo "  If build fails because sun.* classes could not be found"
  echo "  you will need to set the JAVA_HOME environment variable"
  echo "  to the installation directory of java."
fi


# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  ONESEC_HOME=`cygpath --path --windows "$ONESEC_HOME"`
  JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  LOCALCLASSPATH=`cygpath --path --windows "$LOCALCLASSPATH"`
  OSSCON_OPTS="$OSSCON_OPTS -Dcygwin.user.home="`cygpath --path --windows "$HOME"`
fi

if [ ! -d ${ONESEC_HOME}/log ]; then
	mkdir ${ONESEC_HOME}/log 
fi

echo "LOCALCLASSPATH ($LOCALCLASSPATH)"
echo
echo "JAVACMD : $JAVACMD"
cd $ONESEC_HOME
case "$1" in 
'stop')
		echo "can't stop :("
	   ;;
     *)
		"$JAVACMD" $OSSCON_OPTS -Xmx256M -classpath ".:$LOCALCLASSPATH" \
			-Donesec.home="${ONESEC_HOME}" org.onesec.server.helpers.StartServer "$@"\
			>> ${ONESEC_HOME}/log/onesec.out 2>&1 &
esac
