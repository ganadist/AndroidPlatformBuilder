$(info build info:)
$(info ANDROID_JAVA_HOME=$(ANDROID_JAVA_HOME))
$(info PATH=$(PATH))
$(info TARGET_PRODUCT=$(TARGET_PRODUCT))
$(info CM_BUILD=$(CM_BUILD))
$(info BUILD_WITH_COLORS=$(BUILD_WITH_COLORS))
$(info TARGET_BUILD_VARIANT=$(TARGET_BUILD_VARIANT))
$(info ONE_SHOT_MAKEFILE=$(ONE_SHOT_MAKEFILE))
$(info )

droid:
	$(info running droid rule)
	$(CC) -c src/hello.c -o src/hello.o
	javac src/hello.java

systemimage:
	$(info running systemimage)

userdataimage:
	$(info running userdataimage)

vendorimage:
	$(info running vendorimage)

showcommands:


all_modules:
	$(info running mm)
	$(CC) -c src/hello.c -o src/hello.o
	javac src/hello.java

clean:
	rm -f src/hello.o src/hello.class



