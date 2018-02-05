# xmPhoto
获取图片地理位置信息
第一步：要有一个密钥文件，没有就创建一个
第二步：local.properties中添加内容：
filepath=密钥路径
storePassword=store密码
keyAlias=别名
keyPassword=key密码
第三步：高德地图上创建一个地图密钥，替换AndroidManifest.xml中作为地图的key
第四步：正常跑起来