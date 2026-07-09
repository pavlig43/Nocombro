-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# YDB/gRPC/Jackson providers are loaded through ServiceLoader.
-keep class com.fasterxml.jackson.core.JsonFactory { *; }
-keep class com.fasterxml.jackson.databind.ObjectMapper { *; }
-keep class io.grpc.internal.DnsNameResolverProvider { *; }
-keep class io.grpc.internal.PickFirstLoadBalancerProvider { *; }
-keep class io.grpc.netty.shaded.io.grpc.netty.NettyChannelProvider { *; }
-keep class io.grpc.netty.shaded.io.grpc.netty.NettyServerProvider { *; }
-keep class io.grpc.netty.shaded.io.grpc.netty.UdsNameResolverProvider { *; }
-keep class io.grpc.netty.shaded.io.grpc.netty.UdsNettyChannelProvider { *; }
-keep class io.grpc.util.OutlierDetectionLoadBalancerProvider { *; }
-keep class io.grpc.util.SecretRoundRobinLoadBalancerProvider$Provider { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class org.bouncycastle.** { *; }
-keep class tech.ydb.jdbc.YdbDriver { *; }
-keep class tech.ydb.proto.** { *; }
-keepclassmembers class * {
    public static *** getDefaultInstance(...);
}

# YDB chooses its channel factory with Class.forName.
-keep class io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder { *; }
-keep class io.grpc.netty.shaded.** { *; }
-keep class io.grpc.netty.NettyChannelBuilder { *; }
-keep class tech.ydb.core.impl.pool.ChannelFactoryLoader$FactoryLoader { *; }

# Optional Netty/YDB paths that are absent from the Android APK.
-dontwarn io.grpc.netty.**
-dontwarn io.netty.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.eclipse.jetty.**
-dontwarn reactor.blockhound.**
