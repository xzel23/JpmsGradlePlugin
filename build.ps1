# store current directory location
Push-Location .

# determine script location
$BASE_DIR = $PSScriptRoot

# compile the plugin
Set-Location -ErrorAction Stop $BASE_DIR/JpmsGradlePlugin
../gradlew --no-daemon clean build publishToMavenLocal 

if ( -not $? ) { 
    Write-Error "Error compiling plugin"
    Pop-Location
    exit
}

# test the plugin - 1. compile
Set-Location -ErrorAction Stop $BASE_DIR/TestJpmsGradlePlugin
../gradlew --no-daemon clean build --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true

if ( -not $? ) { 
    Write-Error "Error compiling plugin"
    Pop-Location
    exit
}

../gradlew --no-daemon test

if ( -not $? ) { 
    Write-Error "Error running unit test"
    Pop-Location
    exit
}

../gradlew --no-daemon run

if ( -not $? ) { 
    Write-Error "Error running test program"
    Pop-Location
    exit
}

#
../gradlew --no-daemon eclipse --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true

if ( -not $? ) { 
    Write-Error "Error running 'eclipse' task"
    Pop-Location
    exit
}

"Compilation of plugin and test successful!"

# restore current directory location
Pop-Location