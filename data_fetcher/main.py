import sys
from datetime import datetime
from datetime import timedelta
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import matplotlib.pyplot as plt


def get_time_period(literal):
    intervals = {
        'cycle': timedelta(minutes=10),
        'hour': timedelta(hours=1),
        'day': timedelta(days=1),
        'week': timedelta(days=7),
        'month': timedelta(days=30),
    }

    return intervals[literal]


def print_usage():
    print("Correct usage:")
    print("./NAME.py NODE_MAC INTERVAL")
    print("INTERVAL: cycle(10 minutes), hour, day, week, month")


def main():

    if len(sys.argv) < 3:
        print_usage()
        exit()

    # firebase setup
    cred = credentials.Certificate("firebase_key.json")
    firebase_admin.initialize_app(cred, {
        'databaseURL': 'https://bluetooth-sensors-default-rtdb.europe-west1.firebasedatabase.app/'
    })

    ref = db.reference('/logs/node_' + sys.argv[1])
    if ref.get() is None:
        print("Node with MAC " + sys.argv[1] + " not found")
        exit()

    # plot setup
    plt.figure(figsize=(20, 12))

    plt.subplot(2, 2, 1)
    plt.title('Temperature')
    plt.ylabel('$°C$', loc='center', rotation=90)
    plt.xticks(rotation=45, fontsize=8)

    plt.subplot(2, 2, 2)
    plt.title('Pressure')
    plt.ylabel('$hPa$', loc='center', rotation=90)
    plt.ticklabel_format(useOffset=False)
    plt.xticks(rotation=45, fontsize=8)
    plt.yticks(fontsize=8)

    plt.subplot(2, 2, 3)
    plt.title('CH4')
    plt.axhline(y=75, linestyle='--', color='r', label='CH4 danger threshold')
    plt.ylabel('$ppm$', loc='center', rotation=90)
    plt.xticks(rotation=45, fontsize=8)
    plt.legend(loc='upper right')

    plt.subplot(2, 2, 4)
    plt.title('CO')
    plt.axhline(y=1000, linestyle='--', color='r', label='CO danger threshold')
    plt.ylabel('$ppm$', loc='center', rotation=90)
    plt.xticks(rotation=45, fontsize=8)
    plt.legend(loc='upper right')

    # fetching and plotting data
    now = datetime.today()
    for key, value in ref.get().items():
        date = datetime.strptime(key, '%Y-%m-%d-%H-%M-%S')
        delta = now - date
        if delta < get_time_period(sys.argv[2]):
            plt.subplot(2, 2, 1)

            plt.plot(date, value['temperature'], 'b.')

            plt.subplot(2, 2, 2)
            plt.plot(date, value['pressure'], 'b.')

            plt.subplot(2, 2, 3)
            plt.plot(date, value['ch4'], 'b.')

            plt.subplot(2, 2, 4)
            plt.plot(date, value['co'], 'b.')

    plt.suptitle('Readings for last '
                 + sys.argv[2]
                 + ' on '
                 + now.strftime('%Y/%m/%d, %H:%M:%S'), fontsize=24)
    plt.savefig('data_' + now.strftime('%Y_%m_%d_%H_%M_%S') + '.png')


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    main()
