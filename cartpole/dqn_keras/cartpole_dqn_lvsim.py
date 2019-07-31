#import pylab
import random
import numpy as np
import socket
import sys
import time

from collections import deque
from keras.layers import Dense
from keras.optimizers import Adam
from keras.models import Sequential

EPISODES = 300
time_limit = 15
play_type = 0 #pendulum: 0, swing up:1
#play_type = 1 #swing up

if play_type ==0: #inverted
    stop_score = 1500
    break_score = 500
    weight_file = "./save_model/cartpole_dqn_lvsim.h5"

elif play_type == 1: #swing up
    stop_score = 50000
    break_score = 1000
    weight_file = "./save_model/cartpole_dqn_swingup.h5"


# 카트폴 예제에서의 DQN 에이전트
class DQNAgent:
    def __init__(self, state_size, action_size):
        self.load_model = True#False

        # 상태와 행동의 크기 정의
        self.state_size = state_size
        self.action_size = action_size

        # DQN 하이퍼파라미터
        self.discount_factor = 0.99
        self.learning_rate = 0.001
        self.epsilon = 0.0001#1~0.1
        self.epsilon_decay = 0.999
        self.epsilon_min = 0.01
        self.batch_size = 64
        self.train_start = 1000

        # 리플레이 메모리, 최대 크기 2000
        self.memory = deque(maxlen=2000)

        # 모델과 타깃 모델 생성
        self.model = self.build_model()
        self.target_model = self.build_model()

        # 타깃 모델 초기화
        self.update_target_model()

        if self.load_model:
            self.model.load_weights(weight_file)

    # 상태가 입력, 큐함수가 출력인 인공신경망 생성
    def build_model(self):
        model = Sequential()
        model.add(Dense(24, input_dim=self.state_size, activation='relu',
                        kernel_initializer='he_uniform'))
        model.add(Dense(24, activation='relu',
                        kernel_initializer='he_uniform'))
        model.add(Dense(self.action_size, activation='linear',
                        kernel_initializer='he_uniform'))
        model.summary()
        model.compile(loss='mse', optimizer=Adam(lr=self.learning_rate))
        return model

    # 타깃 모델을 모델의 가중치로 업데이트
    def update_target_model(self):
        self.target_model.set_weights(self.model.get_weights())

    # 입실론 탐욕 정책으로 행동 선택
    def get_action(self, state):
        if np.random.rand() <= self.epsilon:
            return random.randrange(self.action_size)
        else:
            q_value = self.model.predict(state)
            return np.argmax(q_value[0])

    # 샘플 <s, a, r, s'>을 리플레이 메모리에 저장
    def append_sample(self, state, action, reward, next_state, done):
        self.memory.append((state, action, reward, next_state, done))

    # 리플레이 메모리에서 무작위로 추출한 배치로 모델 학습
    def train_model(self):
        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay

        # 메모리에서 배치 크기만큼 무작위로 샘플 추출
        mini_batch = random.sample(self.memory, self.batch_size)

        states = np.zeros((self.batch_size, self.state_size))
        next_states = np.zeros((self.batch_size, self.state_size))
        actions, rewards, dones = [], [], []

        for i in range(self.batch_size):
            states[i] = mini_batch[i][0]
            actions.append(mini_batch[i][1])
            rewards.append(mini_batch[i][2])
            next_states[i] = mini_batch[i][3]
            dones.append(mini_batch[i][4])

        # 현재 상태에 대한 모델의 큐함수
        # 다음 상태에 대한 타깃 모델의 큐함수
        target = self.model.predict(states)
        target_val = self.target_model.predict(next_states)

        # 벨만 최적 방정식을 이용한 업데이트 타깃
        for i in range(self.batch_size):
            if dones[i]:
                target[i][actions[i]] = rewards[i]
            else:
                target[i][actions[i]] = rewards[i] + self.discount_factor * (
                    np.amax(target_val[i]))

        self.model.fit(states, target, batch_size=self.batch_size,
                       epochs=1, verbose=0)

def tcp_send(sock, msg):
    msg_len = len(msg)
    msg = str(msg_len) + msg
    sock.send(msg.encode('utf-8'))

def tcp_recv(sock):
    msg_len_raw = sock.recv(2)  # data size receive
    msg_len = msg_len_raw.decode("utf-8", "ignore")
    msg_raw = sock.recv(int(msg_len))  # data receive
    msg = msg_raw.decode("utf-8", "ignore")
    tcp_data = np.fromstring(msg, dtype=np.float, sep=' ')
    return tcp_data

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sock.connect(('localhost', 8585))
#sock.connect(('172.22.11.2', 8585))

if __name__ == "__main__":

    state_size = 4
    action_size = 2

    pre_score = 0

    # LabVIEW TCP 통신
    a_msg = ' f_' + 'first tcp'
    tcp_send(sock, a_msg)
    print('초기 명령 전달')

    tcp_data = tcp_recv(sock)
    print('tcp data= ', tcp_data)

    ######  대기상태 확인 #################
    if tcp_data[5] == -1:
        print('/////msg= ', tcp_data)
        #print('t/f ', tcp_data[5] == 0)
        while tcp_data[5] == -1:
            a_msg = ' r_' + 'ready!...-'

            tcp_send(sock, a_msg)
            tcp_data = tcp_recv(sock)
            print('첫음 준비 단계 tcp_data= ', tcp_data)
     #######################   준비완료   ########################
    print('cartpole agent 준비완료')
    state = tcp_data[1:5]

    # DQN 에이전트 생성
    agent = DQNAgent(state_size, action_size)
    scores, episodes = [], []

    np.set_printoptions(formatter={'foat':'{:0.3f}'.format})
    ##################    에피소드 시작 ##########################
    for e in range(EPISODES):
        print('\n에피소드 시작 ',e,'-th')
        done = False
        score = 0

        start_time = time.time()

        state = np.reshape(state, [1, state_size])

        a_msg = ' i_' + 'run' #+'score_'+str(score)
        tcp_send(sock, a_msg)

        tcp_data = tcp_recv(sock)
        state = tcp_data[1:5]

        count_lv = tcp_data[0]
        posX = tcp_data[1]
        vel = tcp_data[2]
        angle = tcp_data[3]
        W = tcp_data[4]
        done = tcp_data[5]
        pos_err = tcp_data[6]

        ####### 준비상태 확인    ###################
        print('if tcp_data[5]==1 loop start= ')
        if done:    ######  b_done ==1 이면 대기
            print('/////msg= ', a_msg)
            print('done= ', done)
            while done:
                a_msg = ' r_' + 'ready for start!...-'+ \
                    str(pre_score) + ',curr-score_'+str(score) + ','
                tcp_send(sock, a_msg)
                tcp_data = tcp_recv(sock)
                state = tcp_data[1:5]
                done = tcp_data[5]

        done = tcp_data[5]

        print('before while not done= ', done)
        #print('before while not b_done= ', b_done)

        state = np.reshape(state, [1, state_size])
   ########  제어시작    ################################
        print('tcp data= ', tcp_data.round(3))
        #print('state= ''{:0.3f}'.format(tcp_data))
        count =0
        while not done:
            curr_time = time.time()
            # 현재 상태로 행동을 선택
            action = agent.get_action(state)

            a_msg = ',a_'+ str(action) +',pre-score_'+ \
                    str(pre_score) + ',curr-score_'+str(score) + ','
            tcp_send(sock, a_msg)
            tcp_data = tcp_recv(sock)

            next_state = tcp_data[1:5]
            #print('outof range ',tcp_data[6])

            count_lv = tcp_data[0]
            posX = tcp_data[1]
            vel = tcp_data[2]
            angle = tcp_data[3]
            W = tcp_data[4]
            done = tcp_data[5]
            pos_err = tcp_data[6]

            timer = curr_time - start_time

            reward = 1

            next_state = np.reshape(next_state, [1, state_size])

            # 에피소드가 중간에 끝나면 -100 보상
            #reward = reward if not done or score < 499 else -100

            # 리플레이 메모리에 샘플 <s, a, r, s'> 저장
            agent.append_sample(state, action, reward, next_state, done)

            # 매 타임스텝마다 학습
            if len(agent.memory) >= agent.train_start:
                agent.train_model()

            score += reward
            state = next_state

            print('episode=','{0:03}'.format(e),'count=','{0:03}'.format(count),
                  'reward=','{0:03}'.format(reward),'score=','{0:03}'.format(score),
                  'timer=','{0:.1f}'.format(timer),'angle=','{0:2.1f}'.format(angle),
                  'done=','{0:03}'.format(done))

            #print('\t\tscore= ','{0:.1f}'.format(score))

            if done:
                # 각 에피소드마다 타깃 모델을 모델의 가중치로 업데이트
                agent.update_target_model() ############~~~~~##########
                print('before score= ', score)

                #score = score if score > 500 else score + 100
                # 에피소드마다 학습 결과 출력
                print('after score= ', score)
                pre_score = score

                scores.append(score)
                episodes.append(e)
                #pylab.plot(episodes, scores, 'b')
                #pylab.savefig("./save_graph/cartpole_dqn.png")
                print("episode:", e, "  score:", score, "  memory length:",
                      len(agent.memory), "  epsilon:", agent.epsilon)

                # 이전 10개 에피소드의 점수 평균이 490보다 크면 학습 중단
                if np.mean(scores[-min(10, len(scores)):]) > stop_score: #490
                    agent.model.save_weights(weight_file)
                    sys.exit()
            count = count +1
            end_time = time.time()
            #print('total dt= ','{0:.3f}'.format(end_time-start_time))

    sock.close()

    agent.model.save_weights(weight_file)