import socket
import serial
import time
import netifaces as ni

HOST = ""
PORT = 7777
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
print('Socket created ok')

s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

s.bind((HOST, PORT))
print('socket bind ok')

s.listen(1)
print('Socket now listening')

print('IP: ', ni.ifaddresses('wlan0')[2][0]['addr'])

ser = serial.Serial('/dev/ttyACM0', 19200)
print('Serial open')

state = True

start = time.time()

def parsing_input(incmd):
    cmd = bytearray(incmd,'utf8')
    dir = incmd[1:1]
    speed = incmd[3:]

    ser.write(cmd)

    if incmd == "C":
        incmd = "***"
        state = False
        ser.write(b'C')
        
    else:
        incmd = incmd[1:1] + " speed: "+incmd[3:]    

    return incmd


while state:
    conn, addr = s.accept()
    print("\n Linked by ", addr)

    data = conn.recv(128)
    data = data.decode("utf8").strip()
    if not data:
        print("break")
        break
    print("Get: " +data)

    current = time.time()
    dt = current - start
    print("dt= %0.2f" %dt)

    buf = parsing_input(data)
    start = time.time()
    print("pi control: " + buf)

    conn.sendall(buf.encode("utf-8"))
    conn.close()

    if data=="C":
        state = False
    
ser.close()
print("serial to arduino closed")

s.close()
print("socket closed")
