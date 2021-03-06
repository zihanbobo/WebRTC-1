package com.webrtc.arti.actionhandler;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import com.webrtc.arti.common.AnswerType;
import com.webrtc.arti.core.ArtiSwitchBoard;
import com.webrtc.arti.core.ArtiSwitchBoardManager;
import com.webrtc.arti.domain.Client;
import com.webrtc.arti.domain.Server;
import com.webrtc.arti.domain.Session;

/**
 * 对外部用户、客服对人工总机调度模块 提出的动作进行处理
 * 基类
 */
public abstract class ActionHandler 
{
	/**
	 * 需被实现的特色方法
	 */
	//人工总机模块根据不同动作进行不同的调度工作
	public abstract void dispatch(Client client,Queue<Client> requestClientQueue,Map<String, Server> allServerMap,PriorityQueue<Server> idleServerQueue,Map<String, Server> busyServerMap);
	public abstract int getType();
	
	/**
	 * 共用方法
	 */	
	//给用户分配客服
	public void giveOffer(Queue<Client> requestClientQueue,Map<String, Server> allServerMap,PriorityQueue<Server> idleServerQueue,Map<String, Server> busyServerMap)
	{		
		//判断是否还有空闲客服
		if(idleServerQueue.size() > 0 && !requestClientQueue.isEmpty()) //正好有空闲客服 && 等待队列有人
		{			
			//立即分配客服资源
			Server server = idleServerQueue.poll(); //队首的空闲客服出队
			Client client = requestClientQueue.poll(); //队首的等待用户出队
			
			//client-server产生关系
			server.receive(client); //接收用户
			
			if(server.getServingClientNum() >= server.getMaxServingNum()) //达到最大服务数量上限
			{
				busyServerMap.put(server.getUsername(),server); //该客服转移至忙碌表
			}
			else //还有服务数量空余
			{
				idleServerQueue.offer(server); //该客服继续根据优先级进入空闲队列		
			}
			
			/**
			 * 返回前端
			 */	
			//用户得到分配，出队
			ArtiSwitchBoardManager.sendAnswerToWCS(client, AnswerType.SUCCESS); //通知得到分配的用户，客服名为servername
			ArtiSwitchBoardManager.sendAnswerToWCS(server, AnswerType.SUCCESS); //通知得到分配的用户，客服名为servername
			System.out.println("客服："+server.getUsername()+"已被分配出去，服务："+client.getUsername());
			
			ArtiSwitchBoardManager.updateSession(client.getUsername(), server.getUsername());//更新会话
			printAll(requestClientQueue,idleServerQueue, busyServerMap, allServerMap);

			//通知所有剩下在队列等待的用户，等待数量-1
			if(!requestClientQueue.isEmpty()) //不为空 
				ArtiSwitchBoardManager.sendAnswerToWCS(requestClientQueue, AnswerType.UPDATE_REQUESTERNUM);
		}
	}

	
	/**
	 * 调试用！！！！！！！
	 */
	public void printServersInAllMap(Map<String, Server> map)
	{
		System.out.println("=================所有客服Map(用户名,优先级,正服务数)===================");
		for(String key : map.keySet())
		{
			Server server = map.get(key);
			System.out.println(key + "," + server.getPriority() + ","+server.getServingClientNum());
		}
	}
	
	public void printServersInIdleQueue(Queue<Server> queue)
	{
		System.out.println("=================空闲客服Queue(用户名,优先级,正服务数)===================");
		for(Server server : queue)
		{
			System.out.println(server.getUsername() + "," + server.getPriority() + ","+server.getServingClientNum());
		}
	}
	
	public void printServersInBusyMap(Map<String, Server> map)
	{
		System.out.println("=================满员客服Map(用户名,优先级,正服务数)===================");
		for(String key : map.keySet())
		{
			Server server = map.get(key);
			System.out.println(key + "," + server.getPriority() + ","+server.getServingClientNum());
		}
	}
	
	//打印所有请求服务的用户
	public void printClientsInRequestQueue(Queue<Client> requestClientQueue)
	{
		System.out.println("=================等待用户Queue(用户名)===================");
		for(Client client : requestClientQueue)
		{
			System.out.println(client.getUsername());
		}
	}
	
	//打印当前所有客服
	public void printAll(Queue<Client> requestClientQueue,Queue<Server> idleServerQueue,Map<String, Server> busyServerMap,Map<String, Server> allServerMap)
	{
		printClientsInRequestQueue(requestClientQueue);
		printServersInIdleQueue(idleServerQueue);
		printServersInBusyMap(busyServerMap);
		printServersInAllMap(allServerMap);
	}

}
