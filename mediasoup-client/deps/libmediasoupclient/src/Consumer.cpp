#define MSC_CLASS "Consumer"

#include "Consumer.hpp"
#include "Logger.hpp"
#include "MediaSoupClientErrors.hpp"

using json = nlohmann::json;

namespace mediasoupclient
{
	Consumer::Consumer(
	  Consumer::PrivateListener* privateListener,
	  Consumer::Listener* listener,
	  const std::string& id,
	  const std::string& localId,
	  const std::string& producerId,
	  webrtc::RtpReceiverInterface* rtpReceiver,
	  webrtc::MediaStreamTrackInterface* track,
	  const json& rtpParameters,
	  const json& appData)
	  : privateListener(privateListener), listener(listener), id(id), localId(localId),
	    producerId(producerId), rtpReceiver(rtpReceiver), track(track), rtpParameters(rtpParameters),
	    appData(appData)
	{
		MSC_TRACE();
	}

	const std::string& Consumer::GetId() const
	{
		MSC_TRACE();

		return this->id;
	}

	const std::string& Consumer::GetLocalId() const
	{
		MSC_TRACE();

		return this->localId;
	}

	const std::string& Consumer::GetProducerId() const
	{
		MSC_TRACE();

		return this->producerId;
	}

	bool Consumer::IsClosed() const
	{
		MSC_TRACE();

		return this->closed;
	}

	const std::string Consumer::GetKind() const
	{
		MSC_TRACE();

		return this->track->kind();
	}

	webrtc::RtpReceiverInterface* Consumer::GetRtpReceiver() const
	{
		MSC_TRACE();

		return this->rtpReceiver;
	}

	webrtc::MediaStreamTrackInterface* Consumer::GetTrack() const
	{
		MSC_TRACE();

		return this->track;
	}

	const json& Consumer::GetRtpParameters() const
	{
		MSC_TRACE();

		return this->rtpParameters;
	}

	bool Consumer::IsPaused() const
	{
		MSC_TRACE();

		return !this->track->enabled();
	}

	json& Consumer::GetAppData()
	{
		MSC_TRACE();

		return this->appData;
	}

	/**
	 * Closes the Consumer.
	 */
	void Consumer::Close()
	{
		MSC_TRACE();

		if (this->closed)
			return;

		this->closed = true;

		this->privateListener->OnClose(this);
	}

	json Consumer::GetStats() const
	{
		if (this->closed)
			MSC_THROW_INVALID_STATE_ERROR("Consumer closed");

		return this->privateListener->OnGetStats(this);
	}

	/**
	 * Pauses sending media.
	 */
	void Consumer::Pause()
	{
		MSC_TRACE();

		if (this->closed)
		{
			MSC_ERROR("Consumer closed");

			return;
		}

		this->track->set_enabled(false);
	}

	/**
	 * Resumes sending media.
	 */
	void Consumer::Resume()
	{
		MSC_TRACE();

		if (this->closed)
		{
			MSC_ERROR("Consumer closed");

			return;
		}

		this->track->set_enabled(true);
	}

	/**
	 * Sets the frame decryptor for the RTP receiver.
	 */
	void Consumer::SetFrameDecryptor(webrtc::FrameDecryptorInterface* decryptor)
	{
		MSC_TRACE();

		if (this->closed)
			MSC_THROW_INVALID_STATE_ERROR("Consumer closed");

		// 获取底层的 RtpReceiver 并设置解密器
		if (this->rtpReceiver != nullptr)
		{
			// 将原始指针转换为 scoped_refptr
			rtc::scoped_refptr<webrtc::FrameDecryptorInterface> decryptor_refptr(
				decryptor);
			this->rtpReceiver->SetFrameDecryptor(decryptor_refptr);
			MSC_DEBUG("Frame decryptor set on RTP receiver: %p", decryptor);
		}
		else
		{
			MSC_WARN("RTP receiver is null, cannot set frame decryptor");
		}
	}

	/**
	 * Transport was closed.
	 */
	void Consumer::TransportClosed()
	{
		MSC_TRACE();

		if (this->closed)
			return;

		this->closed = true;

		this->listener->OnTransportClose(this);
	}
} // namespace mediasoupclient
