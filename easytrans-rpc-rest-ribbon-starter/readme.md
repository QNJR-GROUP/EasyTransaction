## PRC-REST-RIBBON调用实现

使用HTTP调用及RIBBON做负载均衡的实现

## RPC方法与HTTP方法的映射

为适配上下文的需求，在调用者及被调用者方都允许设置上下文，如被调用者/服务提供方里WEB-CONTEXT为
	
	/${rootContext}

那么可以为EasyTransaction的请求再加一层WEB-CONTEXT,例如：

	/${easytransContext}
	
那么服务提供方实际提供服务的地址以TCC举例，类似如下：

	curl -X POST http://${appid}/${rootContext}/${easytransContext}/${busCode}/doTry
	curl -X POST http://${appid}/${rootContext}/${easytransContext}/${busCode}/doConfirm
	curl -X POST http://${appid}/${rootContext}/${easytransContext}/${busCode}/doCancel

而客户端/调用方，则需要为某个特定APPID对应的服务设置EasyTrans Call Context:

	/${rootContext}/${easytransContext}
	

样例配置如下：

        easytrans:
		  rpc:
		    rest-ribbon:
	    	  enabled: true
	      	provider:
	        	context: /easytrans
	      	consumer:
	        	trx-test-service:
	          	  context: /easytrans

## 配置RPC调用
RPC调用的配置与原生RIBBON/EUREKA一致，除了IRule这个实现由本框架已被写死，其他都可以改变。
独立实现IRule作用主要是为了实现黏性调用，以提高级联事务的效率。

## 注意
目前实现里，businessIdentifer里指定的timeout无效，因timeout在RestTemplate里不能根据业务设定。
	

