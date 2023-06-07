/*
  timer : Set the sample timing
    timerFlag : Enable or disable the timer
  TimerSample : Total sampling sample
  
  data : The data is the command from the mobile phone 
  counter : Count the sample number

*/

#include <SimpleTimer.h>

SimpleTimer Timer;

int timer;
int timerFlag = 0;
int TimerSample = 0;

void setup() {
  
  // Open the serial port and set the baud
  Serial.begin(115200);

  // Clear the serial buffer data
  while(Serial.available())
    int a = Serial.read();

  // Set the timer that triggered every 40 ms, Fs = 25Hz
  timer = Timer.setInterval(40,GetSignal);
  
  // Set the check system that triggered every 5ms
  Timer.setInterval(5,CounterCheck);
  
  // Disable the timer
  Timer.disable(timer);
}

int data = 0;
int counter = 0;

void loop() {
  
  // Wait the command data from mobile phone
  if(Serial.available()){
    data = Serial.read();
    Timer.enable(timer);                // Enable the timer
    TimerSample = 1500 * (data - 48);   // Set sample point to 1500 * (1 ~ 5 min)  
    timerFlag = 0;                      // Set the timerFlag to 0
  }
  Timer.run();
}
// Send the data to mobile phone
void GetSignal()
{
  int data = analogRead(A0);
  Serial.write(data >> 8);
  Serial.write((byte)data);
  // Serial.println(data >> 8);
  // Serial.println((byte)data);
  counter++;
}
// Check the sample point
void CounterCheck()
{
  if(counter == TimerSample){
    
    Timer.disable(timerFlag);
    counter = 0;
  }
}
