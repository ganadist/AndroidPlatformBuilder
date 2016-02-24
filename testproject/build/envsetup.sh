lunch() {
  export ANDROID_PRODUCT_OUT=${OUT}/target/product/generic
}

export LUNCH_MENU_CHOICES=(generic-userdebug aosp_arm-userdebug aosp_mako-userdebug aosp_hammerhead-userdebug)
