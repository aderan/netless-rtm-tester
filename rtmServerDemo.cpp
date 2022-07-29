#include <iostream>
#include <memory>
#include <vector>
#include <string>
#include <thread>
#include <sstream>
#include <unistd.h>
#include <pthread.h>
#include <vector>
#include <map>
#include <algorithm>

#include "IAgoraRtmService.h"

using namespace std;

string APP_ID = "a185de0a777f4c159e302abcc0f03b64";

string CHANNEL_ID = "859edc55-5a4e-4066-abb6-afac5c5ab4e1";

class RtmEventHandler: public agora::rtm::IRtmServiceEventHandler {
  public:
    RtmEventHandler() {}
    ~RtmEventHandler() {}

    virtual void onLoginSuccess() override {
        cout << "on login success" << endl;
    }

    virtual void onLoginFailure(agora::rtm::LOGIN_ERR_CODE errorCode) override {
        cout << "on login failure: errorCode = " << errorCode << endl;
    }

    virtual void onLogout(agora::rtm::LOGOUT_ERR_CODE errorCode) override {
        cout << "on login out" << endl;
    }

    virtual void onConnectionStateChanged(agora::rtm::CONNECTION_STATE state,
                        agora::rtm::CONNECTION_CHANGE_REASON reason) override {
        cout << "on connection state changed: state = " << state << endl;
    }

    virtual void onSendMessageResult(long long messageId,
                        agora::rtm::PEER_MESSAGE_ERR_CODE state) override {
        cout << "on send message messageId: " << messageId << " state: "
             << state << endl;
    }

    virtual void onMessageReceivedFromPeer(const char *peerId,
                        const agora::rtm::IMessage *message) override {
        cout << "on message received from peer: peerId = " << peerId
             << " message = " << message->getText() << endl;
    }
};

class ChannelEventHandler: public agora::rtm::IChannelEventHandler {
  public:
    ChannelEventHandler(string channel) {
        channel_ = channel;    
    }
    ~ChannelEventHandler() {}

    virtual void onJoinSuccess() override {
        cout << "on join channel success" << endl;
    }

    virtual void onJoinFailure(agora::rtm::JOIN_CHANNEL_ERR errorCode) override{
        cout << "on join channel failure: errorCode = " << errorCode << endl;
    }

    virtual void onLeave(agora::rtm::LEAVE_CHANNEL_ERR errorCode) override {
        cout << "on leave channel: errorCode = " << errorCode << endl;
    }

    virtual void onMessageReceived(const char* userId,
                        const agora::rtm::IMessage *msg) override {
        cout << "receive message from channel: " << channel_.c_str()
             << " user: " << userId << " message: " << msg->getText()
             << endl;
    }

    virtual void onMemberJoined(agora::rtm::IChannelMember *member) override {
        cout << "member: " << member->getUserId() << " joined channel: "
             << member->getChannelId() << endl;
    }

    virtual void onMemberLeft(agora::rtm::IChannelMember *member) override {
        cout << "member: " << member->getUserId() << " lefted channel: "
             << member->getChannelId() << endl;
    }

    virtual void onGetMembers(agora::rtm::IChannelMember **members,
                    int userCount,
                    agora::rtm::GET_MEMBERS_ERR errorCode) override {
        cout << "list all members for channel: " << channel_.c_str()
             << " total members num: " << userCount << endl;
        for (int i = 0; i < userCount; i++) {
            cout << "index[" << i << "]: " << members[i]->getUserId();
        }
    }

    virtual void onSendMessageResult(long long messageId,
                    agora::rtm::CHANNEL_MESSAGE_ERR_CODE state) override {
        cout << "send messageId: " << messageId << " state: " << state << endl;
    }
    
    private:
        string channel_;
};

class Demo {
  public:
    Demo() {
        eventHandler_.reset(new RtmEventHandler());
        agora::rtm::IRtmService* p_rs = agora::rtm::createRtmService();
        rtmService_.reset(p_rs, [](agora::rtm::IRtmService* p) {
            p->release();                                                           
        });                                                                         

        if (!rtmService_) {
            cout << "rtm service created failure!" << endl;
            exit(0);
        }

        if (rtmService_->initialize(APP_ID.c_str(), eventHandler_.get())) {
            cout << "rtm service initialize failure! appid invalid?" << endl;
            exit(0);
        }
    }
    ~Demo() {
        rtmService_->release();
    }

  public:
    bool login( const std::string& userId, const std::string& token) {
        cout << "call login token:" << token << " userId:" << userId 
             << endl;
        if (rtmService_->login(token.c_str(), userId.c_str())) {
            cout << "login failed!" << endl;
            return false;
        }
        cout << "here" << endl;
        return true;
    }

    void logout() {
        rtmService_->logout();
        cout << "log out!" << endl;
    }

    void p2pChat(const std::string& dst) {
        string msg;
        while(true) {
            cout << "please input message you want to send, or input \"quit\" "
                 << "to leave p2pChat" << endl;
            getline(std::cin, msg);
            if (msg.compare("quit") == 0) {
                return;
            } else {
                sendMessageToPeer(dst, msg);
            }
        }
    }

    void joinChannel(const std::string& channelId) {
        channelEvent_.reset(new ChannelEventHandler(channelId));
        agora::rtm::IChannel * messageChannel = rtmService_->createChannel(channelId.c_str(), channelEvent_.get());
        if (!messageChannel) {
            cout << "create messageChannel failed!" << endl;
        }
        messageChannel->join();

        string commandChannelId = channelId + "commands";
        agora::rtm::IChannel * commandChannel = rtmService_->createChannel(commandChannelId.c_str(), channelEvent_.get());
        if (!commandChannel) {
            cout << "create commandChannel failed!" << endl;
        }
        commandChannel->join();

        cout << "join channel success" << endl;
    }

    void sendMessageToPeer(std::string peerID, std::string msg) {
        agora::rtm::IMessage* rtmMessage = rtmService_->createMessage();
        rtmMessage->setText(msg.c_str());
        int ret = rtmService_->sendMessageToPeer(peerID.c_str(),
                                        rtmMessage);
        rtmMessage->release();
        if (ret) {
            cout << "send message to peer failed! return code: " << ret
                 << endl;
        }
    }

    void sendMessageToChannel(agora::rtm::IChannel * channelHandler,
                            string &msg) {
        agora::rtm::IMessage* rtmMessage = rtmService_->createMessage();
        rtmMessage->setText(msg.c_str());
        channelHandler->sendMessage(rtmMessage);
        rtmMessage->release();
    }

    private:
        std::unique_ptr<agora::rtm::IRtmServiceEventHandler> eventHandler_;
        std::unique_ptr<ChannelEventHandler> channelEvent_;
        std::shared_ptr<agora::rtm::IRtmService> rtmService_;
};

// for string delimiter
vector<string> split (string s, string delimiter) {
    size_t pos_start = 0, pos_end, delim_len = delimiter.length();
    string token;
    vector<string> res;

    while ((pos_end = s.find (delimiter, pos_start)) != string::npos) {
        token = s.substr (pos_start, pos_end - pos_start);
        pos_start = pos_end + delim_len;
        res.push_back (token);
    }

    res.push_back (s.substr (pos_start));
    return res;
}

int main(int argc, const char * argv[]) {
    // if (argc != 3) {
    //     cout << "Usage: ./rtmServerDemo <Uuid> <Token>" << endl;
    //     exit(-1);
    // }

    vector<string> v = split(argv[1], ":");
    string userId = v[0];
    string token =  v[1];
    
    Demo* demo = new Demo();
    demo->login(userId, token);
    demo->joinChannel(CHANNEL_ID);

   while (true) {
      sleep(1000);
      cout << "Sleep" << endl;
   }
    return 0;
}