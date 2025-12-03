package xyz.ytora.ytool.crypto;

/**
 * created by yangtong on 2025/4/4 下午7:55<br/>
 * <p>
 * RSA+AES 混合加密原理：<br/>
 * 服务端生成一对RAS的公钥和密钥，将公钥发送给客户端<br/>
 * 客户端再生成AES密钥，将这个AES密钥通过RAS公钥加密后，发给服务端<br/>
 * 服务端使用RAS密钥来解密，获得AES密钥<br/>
 * 此时客户端和服务器双方都持有了AES密钥，此后双方通信都可以使用AES密钥来加解密<br/>
 */
public class HybridCryptos {

}
