# store current directory location
Push-Location .

# determine script location
$BASE_DIR = $PSScriptRoot

# compile the plugin
Set-Location -ErrorAction Stop $BASE_DIR/JpmsGradlePlugin
./gradlew --no-daemon clean build publishToMavenLocal 

if ( -not $? ) { 
    Write-Error "Error compiling plugin"
    Pop-Location
    exit
}

# test the plugin
Set-Location -ErrorAction Stop $BASE_DIR/JpmsGradlePlugin
./gradlew --no-daemon eclipse --stacktrace -DDEBUG_JPMS_GRADLE_PLUGIN=true 

if ( -not $? ) { 
    Write-Error "Error compiling plugin"
    Pop-Location
    exit
}

#
"Compilation of plugin and test successful!"

# restore current directory location
Pop-Location