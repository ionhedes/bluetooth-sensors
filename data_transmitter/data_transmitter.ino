#include <SFE_BMP180.h>
#include <Wire.h>
#include <SoftwareSerial.h>

SFE_BMP180 sensor;

const int txPin = 2;
const int rxPin = 3;

SoftwareSerial BTSerial(rxPin, txPin);

typedef union {
  struct {
    double temperature;
    double pressure;
  } values;
  byte bin[8];
} payload;

void setup() {
  Wire.begin();
  Serial.begin(9600);
  BTSerial.begin(9600);
  bool success = sensor.begin();
}

void convert(payload* msg)
{
  byte* aux = msg->bin;
  for (int i = 0; i < 4; i++)
  {
    msg->bin[i] = aux[3 - i];
  }

  for (int i = 4; i < 8; i++)
  {
    msg->bin[i] = aux[11 - i];
  }
}

void loop() {
  char status;
  double temp,pres;
  bool success=false;
  payload data_to_send;

  status = sensor.startTemperature();

  if(status != 0) {
    delay(1000);
    status = sensor.getTemperature(temp);

    if(status != 0){
      status = sensor.startPressure(3);

      if(status != 0){
        delay(status);
        status = sensor.getPressure(pres, temp);

         if(status != 0){
            data_to_send.values.temperature = temp;
            data_to_send.values.pressure = pres;
            convert(&data_to_send);
            BTSerial.write(data_to_send.bin, 8);
            for (int i = 0; i < 8; i++)
            {
              Serial.println(data_to_send.bin[i], BIN);
            }
            Serial.println(temp);
            Serial.println(pres);
            Serial.println("");
            
            Serial.print("\n");
            delay(3000);
         }
      }
    }
  }

}
