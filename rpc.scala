import org.zeromq.ZMQ
import ZHelpers._
object rpctest{
class SendClient(ctx:ZMQ.Context,name:String) extends Runnable{
def run(){
  val socket=ctx.socket(ZMQ.DEALER)
  socket.setIdentity(("client sender "+name).getBytes)
  socket.connect("tcp://localhost:5555")
  var i:Int=0;
  while(true){
    socket.send(("client receiver "+name).getBytes,ZMQ.SNDMORE)
    socket.send("".getBytes,ZMQ.SNDMORE)
    socket.send(("request body "+i).getBytes,0)
    println("clent request "+ i+ " send")
    i=i+1;
    Thread.sleep(1000);
  }
}
}
class Server(ctx:ZMQ.Context) extends Runnable{
def run(){
val socket=ctx.socket(ZMQ.DEALER)
socket.bind("tcp://127.0.0.1:5555")
val responseSocket=ctx.socket(ZMQ.ROUTER)
responseSocket.bind("tcp://127.0.0.1:5556")
while(true){
    val messages = scala.collection.mutable.Queue[Array[Byte]]()
			messages.enqueue(socket.recv(0))
			while (socket.hasReceiveMore) {
				messages.enqueue(socket.recv(0))
			}
    println(messages.map(new String(_)).mkString("|"))
    val msg=messages
    val last=msg.last
    for(frame<-msg if frame!=last){
      responseSocket.send(frame,ZMQ.SNDMORE)
    }
    responseSocket.send(last,0)
}
}
}
class ReceiveClient(ctx:ZMQ.Context,name:String) extends Runnable{
def run(){
  val socket=ctx.socket(ZMQ.DEALER)
  socket.setIdentity(("client receiver "+name).getBytes)
  socket.connect("tcp://localhost:5556")
  while(true){
val messages = scala.collection.mutable.Queue[Array[Byte]]()
                        messages.enqueue(socket.recv(0))
                        while (socket.hasReceiveMore) {
                                messages.enqueue(socket.recv(0))
                        }
    println("client "+name+" received: "+messages.map(new String(_)).mkString("|"))

}
}
}
def main(args:Array[String]){
val clientContext=ZMQ.context(1)
new Thread(new SendClient(clientContext,"A")).start
val serverContext=ZMQ.context(1)
new Thread(new Server(serverContext)).start
new Thread(new ReceiveClient(clientContext,"A")).start
new Thread(new ReceiveClient(clientContext,"B")).start
}
}
