
String data, dir, str_speed, lens;
char chdata, strbuff;
int dt=5;
int speed=0;
int speed_steer=0;
int x=0, len=0, pos=0;

void setup(){
 Serial.begin(19200);
 pinMode(7, OUTPUT);       // Motor A 방향설정1
 pinMode(8, OUTPUT);       // Motor A 방향설정2
 pinMode(4, OUTPUT);       // Motor B 방향설정1
 pinMode(5, OUTPUT);       // Motor B 방향설정2
 pinMode(13, OUTPUT);
}

void loop(){
  digitalWrite(13, LOW);
  delay(dt);
  digitalWrite(13, HIGH);
  delay(dt);
  
  
  if(Serial.available() > 0){
    delay(1);
    data = Serial.readStringUntil('T');
    Serial.println(data);
    dir = data.substring(1,2);
    //str_speed = data.substring(3,6);
    
    if(dir == "G"){
      str_speed = data.substring(3,6);
      speed = str_speed.toInt();
    }
    if(dir == "B"){
      str_speed = data.substring(3,6);
      speed = str_speed.toInt(); 
    }
    
    //Serial.println(data);
    //Serial.println(dir);
    
    if (dir == "G"){
      analogWrite(3, speed);       // Motor A 속도조절 (0~255)
      //Serial.println(speed);
      if (speed >70){
        digitalWrite(4, HIGH);
        digitalWrite(5, LOW);
      }else{
        digitalWrite(4, LOW);
        digitalWrite(5, LOW); 
      }
    }
    
      if (dir == "B"){
        //Serial.println(speed);
        analogWrite(3, speed);       // Motor B속도조절 (0~255)
        
        if (speed >70){
          digitalWrite(4, LOW);
          digitalWrite(5, HIGH);
        }else{
          digitalWrite(4, LOW);
          digitalWrite(5, LOW); 
      }
    }
    
      if (dir == "L"){
        //Serial.println(speed);
        str_speed = data.substring(3,6);
        speed_steer = str_speed.toInt();          
        analogWrite(9, speed_steer);       // Motor2 steer조절 (0~255)
              
        if (speed_steer >70){
          digitalWrite(7, LOW);
          digitalWrite(8, HIGH);
        }else{
          digitalWrite(7, LOW);
          digitalWrite(8, LOW); 
        }
      }
      
      if (dir == "R"){
        //Serial.println(speed);
        str_speed = data.substring(3,6);
        speed_steer = str_speed.toInt();            
        analogWrite(9, speed_steer);       // Motor B속도조절 (0~255)
        
        if (speed_steer >70){
          digitalWrite(8, LOW);
          digitalWrite(7, HIGH);
        }else{
          digitalWrite(8, LOW);
          digitalWrite(7, LOW); 
        }
      }
    
  }
}

